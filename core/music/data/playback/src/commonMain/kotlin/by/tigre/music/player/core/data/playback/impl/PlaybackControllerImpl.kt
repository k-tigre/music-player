package by.tigre.music.player.core.data.playback.impl

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.media.platform.playback.MediaItemWrapper
import by.tigre.music.player.core.data.playback.ActivePlaybackSource
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.media.platform.playback.PlaybackPlayer
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.entiry.playback.PlaybackInterruption
import by.tigre.music.player.core.entiry.playback.PlayableItem
import by.tigre.music.player.core.entiry.playback.ResumePoint
import by.tigre.music.player.core.entiry.playback.SongInQueueItem
import by.tigre.logger.Log
import by.tigre.logger.extensions.debugLog
import by.tigre.media.platform.tools.coroutines.CoreScope
import by.tigre.media.platform.tools.coroutines.extensions.withLatestFrom
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

    private val playingState = MutableStateFlow(false)
    private val action = MutableSharedFlow<Action>(extraBufferCapacity = 1)
    private val activeSource = MutableStateFlow<ActivePlaybackSource>(ActivePlaybackSource.Session)
    private val interruptionState = MutableStateFlow<PlaybackInterruption?>(null)
    private val overlayItem = MutableStateFlow<PlayableItem.ExternalAudio?>(null)
    private var pendingSessionSeekMs: Long? = null

    override val activePlaybackSource: StateFlow<ActivePlaybackSource> = activeSource.asStateFlow()
    override val interruption: StateFlow<PlaybackInterruption?> = interruptionState.asStateFlow()
    override val nowPlayingOverlay: StateFlow<PlayableItem.ExternalAudio?> = overlayItem.asStateFlow()
    override val isPlaying: StateFlow<Boolean> = playingState.asStateFlow()

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

    override val orderMode: Flow<Boolean> = storage.orderMode.map { it == PlaybackQueueStorage.OrderMode.Normal }

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

                        is Action.ChangeOrderMode -> {
                            storage.setOrderMode(if (action.isNormal) PlaybackQueueStorage.OrderMode.Normal else PlaybackQueueStorage.OrderMode.Random)
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
                        is ActivePlaybackSource.Session -> playNext()
                    }
                }
        }

        scope.launch {
            player.state
                .debounce(10000)
                .filter { it != PlaybackPlayer.State.Playing }
                .withLatestFrom(playingState) { _, playing -> playing }
                .filter { it }
                .debugLog("PlaybackController", "AAAA!!!! Wrong player state")
                .collect { playingState.emit(false) }
        }

        scope.launch {
            var wasPlayerPlaying = player.state.value == PlaybackPlayer.State.Playing
            player.state.collect { state ->
                val isPlayerPlaying = state == PlaybackPlayer.State.Playing
                if (wasPlayerPlaying && !isPlayerPlaying && playingState.value) {
                    playingState.emit(false)
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
                    pendingSessionSeekMs = null
                    player.setMediaItem(songToMediaItem(item.song), seek)
                    if (playingState.value) {
                        player.resume()
                    }
                } else if (activeSource.value is ActivePlaybackSource.Session) {
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
                    if (playingState.value) {
                        player.resume()
                    }
                }
            }
        }

        scope.launch {
            playingState
                .withLatestFrom(activeSource, player.state)
                .debugLog("PlaybackController", "isPlaying")
                .collect { (playing, source, state) ->
                    if (source == null || state == null) return@collect
                    when (source) {
                        is ActivePlaybackSource.Overlay -> when {
                            !playing -> player.pause()
                            playing && state != PlaybackPlayer.State.Idle -> player.resume()
                            playing && state == PlaybackPlayer.State.Idle -> {
                                player.setMediaItem(externalToMediaItem(source.item), 0)
                                player.resume()
                            }
                        }

                        is ActivePlaybackSource.Session -> when {
                            !playing -> player.pause()
                            playing && currentItem.value != null && state != PlaybackPlayer.State.Idle -> player.resume()
                            playing && currentItem.value != null && state == PlaybackPlayer.State.Idle -> {
                                currentItem.value?.let { song ->
                                    player.setMediaItem(songToMediaItem(song), 0)
                                    player.resume()
                                }
                            }
                        }
                    }
                }
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
            overlayItem.value = null
            activeSource.value = ActivePlaybackSource.Session
            interruptionState.value = null

            if (snapshot != null) {
                pendingSessionSeekMs = snapshot.resumePoint.positionMs
                storage.playSongInQueue(snapshot.resumePoint.queueEntryId)
                playingState.emit(snapshot.wasPlaying)
            } else {
                playingState.emit(false)
            }
        }
    }

    override fun setOrderMode(isNormal: Boolean) {
        action.tryEmit(Action.ChangeOrderMode(isNormal))
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
        Log.d("PlaybackController") { "pause -- ${playingState.value}" }
        scope.launch {
            playingState.emit(false)
            player.pause()
        }
    }

    override fun resume() {
        Log.d("PlaybackController") { "resume -- ${playingState.value}" }
        scope.launch {
            playingState.emit(true)
            player.resume()
        }
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
                playingState.emit(true)
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
        scope.launch { player.stop() }
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
        playingState.emit(true)
    }

    private suspend fun captureInterruption(queue: List<PlaybackQueueStorage.QueueItem>) {
        val playingItem = queue.firstOrNull { it.state == PlaybackQueueStorage.QueueItem.State.Playing }
        val targetItem = playingItem ?: queue.first()
        val positionMs = player.progress.first().position
        val wasPlaying = playingState.value && playingItem != null

        interruptionState.value = PlaybackInterruption(
            resumePoint = ResumePoint(
                queueEntryId = targetItem.id,
                positionMs = positionMs,
            ),
            wasPlaying = wasPlaying,
        )
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
        data class ChangeOrderMode(val isNormal: Boolean) : Action
    }
}
