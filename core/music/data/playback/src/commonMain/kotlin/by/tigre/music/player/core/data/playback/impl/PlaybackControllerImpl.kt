package by.tigre.music.player.core.data.playback.impl

import by.tigre.logger.Log
import by.tigre.logger.extensions.debugLog
import by.tigre.media.platform.playback.MediaItemWrapper
import by.tigre.media.platform.playback.PlaybackPlayer
import by.tigre.media.platform.tools.coroutines.CoreScope
import by.tigre.media.platform.tools.coroutines.extensions.withLatestFrom
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playback.ActivePlaybackSource
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.entiry.playback.PlayableItem
import by.tigre.music.player.core.entiry.playback.PlaybackInterruption
import by.tigre.music.player.core.entiry.playback.ResumePoint
import by.tigre.music.player.core.entiry.playback.SongInQueueItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
internal class PlaybackControllerImpl(
    private val storage: PlaybackQueueStorage,
    private val catalog: CatalogSource,
    override val player: PlaybackPlayer,
    private val scope: CoreScope
) : PlaybackController {

    /** Whether the app requested playback; the engine may pause independently (e.g. audio focus). */
    private val shouldPlay = MutableStateFlow(false)
    private val action = MutableSharedFlow<Action>(extraBufferCapacity = 1)
    private val activeSource = MutableStateFlow<ActivePlaybackSource>(ActivePlaybackSource.Session)
    private val interruptionState = MutableStateFlow<PlaybackInterruption?>(null)
    private val overlayItem = MutableStateFlow<PlayableItem.ExternalAudio?>(null)
    private var pendingSessionSeekMs: Long? = null
    private var sessionMediaLoadedForQueueEntryId: Long? = null

    override val activePlaybackSource: StateFlow<ActivePlaybackSource> = activeSource.asStateFlow()
    override val interruption: StateFlow<PlaybackInterruption?> = interruptionState.asStateFlow()
    override val nowPlayingOverlay: StateFlow<PlayableItem.ExternalAudio?> = overlayItem.asStateFlow()
    override val isPlaying: StateFlow<Boolean> = player.state
        .map { it == PlaybackPlayer.State.Playing }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(), initialValue = false)

    private val currentQueueItem: StateFlow<SongInQueueItem?> = storage.currentQueue
        .map { queue ->
            queue.firstOrNull { it.state == PlaybackQueueStorage.QueueItem.State.Playing }
                ?.let { item ->
                    catalog.getSongById(id = item.songsId)
                        ?.let { song ->
                            SongInQueueItem(id = item.id, song = song, isPlaying = true)
                        }
                }

        }
        .debugLog("PlaybackController", "currentItem")
        .stateIn(scope, SharingStarted.WhileSubscribed(), initialValue = null)

    override val currentItem: StateFlow<Song?> = currentQueueItem
        .map { it?.song }
        .stateIn(scope, SharingStarted.WhileSubscribed(), initialValue = null)

    override val currentQueue: Flow<List<SongInQueueItem>> =
        combine(
            storage.currentQueue
                .map {
                    it.map(PlaybackQueueStorage.QueueItem::songsId).distinct()
                }
                .distinctUntilChanged()
                .map { ids ->
                    catalog.getSongsByIds(ids).associateBy { it.id }
                },
            storage.currentQueue
        ) { songs, currentQueue ->
            currentQueue.mapNotNull { item ->
                songs[item.songsId]?.let {
                    SongInQueueItem(
                        id = item.id,
                        song = it,
                        isPlaying = item.state == PlaybackQueueStorage.QueueItem.State.Playing
                    )
                }
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(), initialValue = emptyList())

    override val shuffleEnabled: Flow<Boolean> = storage.shuffleEnabled
    override val repeatMode: Flow<PlaybackQueueStorage.RepeatMode> = storage.repeatMode

    init {
        scope.launch {
            action
                .debugLog("PlaybackController", "action")
                .collect { action ->
                    when (action) {
                        is Action.PlaySong -> {
                            clearOverlayState()
                            storage.playSongs(listOf(action.songId))
                        }

                        is Action.PlayAlbum -> {
                            clearOverlayState()
                            storage.playSongs(catalog.getSongsByAlbum(action.artistId, action.albumId).map(Song::id))
                        }

                        is Action.AddAlbumToQueue -> {
                            storage.addSongs(catalog.getSongsByAlbum(action.artistId, action.albumId).map(Song::id))
                        }

                        is Action.AddSongToQueue -> {
                            storage.addSongs(listOf(action.songId))
                        }

                        is Action.AddArtistToQueue -> {
                            storage.addSongs(catalog.getSongsByArtist(action.artistId).map(Song::id))
                        }

                        is Action.PlayArtist -> {
                            clearOverlayState()
                            storage.playSongs(catalog.getSongsByArtist(action.artistId).map(Song::id))
                        }

                        is Action.ToggleShuffle -> {
                            val enabled = storage.shuffleEnabled.first()
                            storage.setShuffleEnabled(!enabled)
                        }

                        is Action.CycleRepeat -> {
                            val current = storage.repeatMode.first()
                            val next = when (current) {
                                PlaybackQueueStorage.RepeatMode.Off -> PlaybackQueueStorage.RepeatMode.All
                                PlaybackQueueStorage.RepeatMode.All -> PlaybackQueueStorage.RepeatMode.One
                                PlaybackQueueStorage.RepeatMode.One -> PlaybackQueueStorage.RepeatMode.Off
                            }
                            storage.setRepeatMode(next)
                        }

                        Action.PlayNext -> storage.playNext()

                        Action.PlayPrev -> storage.playPrev()

                        is Action.PlaySongInQueue -> storage.playSongInQueue(action.queueId)
                    }
                }
        }

        scope.launch {
            player.state
                .debugLog("PlaybackController", " player.state")
                .filter { it == PlaybackPlayer.State.Ended }
                .collect {
                    when (activeSource.value) {
                        is ActivePlaybackSource.Overlay -> resumeInterruptedSession()
                        is ActivePlaybackSource.Session -> handleSessionEnded()
                    }
                }
        }

        scope.launch {
            player.state
                .debounce(10000)
                .filter { it != PlaybackPlayer.State.Playing }
                .withLatestFrom(shouldPlay) { _, requested -> requested }
                .filter { it }
                .debugLog("PlaybackController", "AAAA!!!! Wrong player state")
                .collect { shouldPlay.emit(false) }
        }

        scope.launch {
            var wasPlayerPlaying = player.state.value == PlaybackPlayer.State.Playing
            player.state.collect { state ->
                val isPlayerPlaying = state == PlaybackPlayer.State.Playing
                if (wasPlayerPlaying && !isPlayerPlaying && shouldPlay.value && state != PlaybackPlayer.State.Ended) {
                    shouldPlay.emit(false)
                }
                wasPlayerPlaying = isPlayerPlaying
            }
        }

        scope.launch {
            combine(currentQueueItem, activeSource) { item, source ->
                if (source is ActivePlaybackSource.Session) item else null
            }.collect { item ->
                if (item != null) {
                    val seek = pendingSessionSeekMs ?: 0L
                    val needsMediaLoad = sessionMediaLoadedForQueueEntryId != item.id || seek > 0L
                    if (needsMediaLoad) {
                        pendingSessionSeekMs = null
                        sessionMediaLoadedForQueueEntryId = item.id
                        player.setMediaItem(songToMediaItem(item.song), seek)
                    }
                    applyPlayback()
                } else if (activeSource.value is ActivePlaybackSource.Session) {
                    sessionMediaLoadedForQueueEntryId = null
                    player.stop()
                }
            }
        }

        scope.launch {
            combine(overlayItem, activeSource) { item, source ->
                if (source is ActivePlaybackSource.Overlay) item else null
            }.collect { item ->
                if (item != null) {
                    player.setMediaItem(externalToMediaItem(item), 0)
                    applyPlayback()
                }
            }
        }

        scope.launch {
            shouldPlay
                .withLatestFrom(activeSource)
                .debugLog("PlaybackController", "applyPlayback")
                .collect { applyPlayback() }
        }
    }

    override fun playExternal(item: PlayableItem.ExternalAudio) {
        Log.d("PlaybackController") { "playExternal" }
        scope.launch { enterOverlay(item) }
    }

    override fun resumeInterruptedSession() {
        Log.d("PlaybackController") { "resumeInterruptedSession" }
        scope.launch {
            val snapshot = interruptionState.value
            if (snapshot != null) {
                pendingSessionSeekMs = snapshot.resumePoint.positionMs
                storage.playSongInQueue(snapshot.resumePoint.queueEntryId)
            }
            overlayItem.value = null
            activeSource.value = ActivePlaybackSource.Session
            interruptionState.value = null

            if (snapshot != null) {
                setShouldPlay(snapshot.wasPlaying)
            } else {
                setShouldPlay(false)
            }
        }
    }

    override fun toggleShuffle() {
        action.tryEmit(Action.ToggleShuffle)
    }

    override fun cycleRepeat() {
        action.tryEmit(Action.CycleRepeat)
    }

    override fun playNext() {
        Log.d("PlaybackController") { "playNext" }
        if (activeSource.value is ActivePlaybackSource.Overlay) {
            resumeInterruptedSession()
        } else {
            action.tryEmit(Action.PlayNext)
        }
    }

    override fun playPrev() {
        Log.d("PlaybackController") { "playPrev" }
        if (activeSource.value is ActivePlaybackSource.Overlay) {
            scope.launch { player.seekTo(0) }
        } else {
            action.tryEmit(Action.PlayPrev)
        }
    }

    override fun pause() {
        Log.d("PlaybackController") { "pause -- ${shouldPlay.value}" }
        scope.launch { setShouldPlay(false) }
    }

    override fun resume() {
        Log.d("PlaybackController") { "resume -- ${shouldPlay.value}" }
        scope.launch { setShouldPlay(true) }
    }

    override fun playSong(id: Song.Id) {
        Log.d("PlaybackController") { "playSong" }
        clearOverlayState()
        action.tryEmit(Action.PlaySong(id))
        resume()
    }

    override fun playSongInQueue(id: Long) {
        Log.d("PlaybackController") { "playSongInQueue" }
        if (activeSource.value is ActivePlaybackSource.Overlay) {
            scope.launch {
                clearOverlayState()
                storage.playSongInQueue(id)
                setShouldPlay(true)
            }
        } else {
            action.tryEmit(Action.PlaySongInQueue(id))
            resume()
        }
    }

    override fun playAlbum(albumId: Album.Id, artistId: Artist.Id) {
        Log.d("PlaybackController") { "playAlbum" }
        clearOverlayState()
        action.tryEmit(Action.PlayAlbum(albumId, artistId))
        resume()
    }

    override fun addAlbumToPlay(id: Album.Id, artistId: Artist.Id) {
        Log.d("PlaybackController") { "addAlbumToPlay" }
        action.tryEmit(Action.AddAlbumToQueue(id, artistId))
    }

    override fun addSongToPlay(id: Song.Id) {
        Log.d("PlaybackController") { "addSongToPlay" }
        action.tryEmit(Action.AddSongToQueue(id))
    }

    override fun playArtist(id: Artist.Id) {
        Log.d("PlaybackController") { "playArtist" }
        clearOverlayState()
        action.tryEmit(Action.PlayArtist(id))
        resume()
    }

    override fun addArtistToPlay(id: Artist.Id) {
        Log.d("PlaybackController") { "addArtistToPlay" }
        action.tryEmit(Action.AddArtistToQueue(id))
    }

    override fun removeSongsFromQueue(ids: List<Song.Id>) {
        scope.launch { storage.removeSongsByIds(ids) }
    }

    override fun stop() {
        Log.d("PlaybackController") { "stop" }
        scope.launch {
            setShouldPlay(false)
            player.stop()
        }
    }

    private suspend fun enterOverlay(item: PlayableItem.ExternalAudio) {
        if (activeSource.value is ActivePlaybackSource.Session) {
            val queue = storage.currentQueue.first()
            if (queue.isNotEmpty()) {
                captureInterruption(queue)
            }
        }
        overlayItem.value = item
        activeSource.value = ActivePlaybackSource.Overlay(item)
        setShouldPlay(true)
    }

    private suspend fun captureInterruption(queue: List<PlaybackQueueStorage.QueueItem>) {
        val playingItem = queue.firstOrNull { it.state == PlaybackQueueStorage.QueueItem.State.Playing }
        val targetItem = playingItem ?: queue.first()
        val positionMs = player.progress.first().position
        val wasPlaying = player.state.value == PlaybackPlayer.State.Playing && playingItem != null

        interruptionState.value = PlaybackInterruption(
            resumePoint = ResumePoint(
                queueEntryId = targetItem.id,
                positionMs = positionMs,
            ),
            wasPlaying = wasPlaying,
        )
    }

    private suspend fun setShouldPlay(value: Boolean) {
        shouldPlay.emit(value)
        applyPlayback()
    }

    private suspend fun applyPlayback() {
        if (!shouldPlay.value) {
            player.pause()
            return
        }
        when (val source = activeSource.value) {
            is ActivePlaybackSource.Overlay -> {
                when (player.state.value) {
                    PlaybackPlayer.State.Idle -> {
                        player.setMediaItem(externalToMediaItem(source.item), 0)
                        player.resume()
                    }
                    else -> player.resume()
                }
            }
            is ActivePlaybackSource.Session -> {
                val song = currentItem.value ?: run {
                    player.pause()
                    return
                }
                when (player.state.value) {
                    PlaybackPlayer.State.Idle -> {
                        val seek = pendingSessionSeekMs ?: 0L
                        pendingSessionSeekMs = null
                        player.setMediaItem(songToMediaItem(song), seek)
                        player.resume()
                    }
                    else -> player.resume()
                }
            }
        }
    }

    private suspend fun handleSessionEnded() {
        when (storage.repeatMode.first()) {
            PlaybackQueueStorage.RepeatMode.One -> {
                player.seekTo(0)
                setShouldPlay(true)
            }

            PlaybackQueueStorage.RepeatMode.All -> action.emit(Action.PlayNext)

            PlaybackQueueStorage.RepeatMode.Off -> {
                val hasNext = storage.currentQueue.first().any { it.state == PlaybackQueueStorage.QueueItem.State.Pending }
                if (hasNext) {
                    action.emit(Action.PlayNext)
                } else {
                    setShouldPlay(false)
                }
            }
        }
    }

    private fun clearOverlayState() {
        overlayItem.value = null
        interruptionState.value = null
        if (activeSource.value is ActivePlaybackSource.Overlay) {
            activeSource.value = ActivePlaybackSource.Session
        }
    }

    private fun songToMediaItem(song: Song): MediaItemWrapper = MediaItemWrapper(
        uri = song.path,
        title = song.name,
        artist = song.artist,
        albumTitle = song.album
    )

    private fun externalToMediaItem(item: PlayableItem.ExternalAudio): MediaItemWrapper = MediaItemWrapper(
        uri = item.uri,
        title = item.title,
        artist = item.sourceLabel,
        albumTitle = null,
    )

    private sealed interface Action {
        data object PlayNext : Action
        data object PlayPrev : Action

        data class PlaySong(val songId: Song.Id) : Action
        data class PlaySongInQueue(val queueId: Long) : Action
        data class PlayAlbum(val albumId: Album.Id, val artistId: Artist.Id) : Action
        data class AddAlbumToQueue(val albumId: Album.Id, val artistId: Artist.Id) : Action
        data class AddSongToQueue(val songId: Song.Id) : Action
        data class PlayArtist(val artistId: Artist.Id) : Action
        data class AddArtistToQueue(val artistId: Artist.Id) : Action
        data object ToggleShuffle : Action
        data object CycleRepeat : Action
    }
}
