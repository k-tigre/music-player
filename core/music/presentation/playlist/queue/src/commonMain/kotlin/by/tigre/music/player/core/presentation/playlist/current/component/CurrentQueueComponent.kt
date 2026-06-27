package by.tigre.music.player.core.presentation.playlist.current.component

import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.data.playlist.AddToPlaylistCoordinator
import by.tigre.music.player.core.data.playlist.AddToPlaylistRequest
import by.tigre.music.player.core.data.playlist.PlaylistRepository
import by.tigre.music.player.core.entiry.playback.NowPlayingQueueEntry
import by.tigre.music.player.core.entiry.playback.NowPlayingScreenModel
import by.tigre.music.player.core.entiry.playback.OverlayQueueEntry
import by.tigre.music.player.core.entiry.playback.QueueSession
import by.tigre.music.player.core.entiry.playback.SongInQueueItem
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueDependency
import by.tigre.music.player.core.presentation.playlist.current.navigation.QueueNavigator
import by.tigre.music.player.core.presentation.playlist.current.util.currentDateForPlaylistName
import `by`.tigre.music.player.core.presentation.queue.resources.Res
import `by`.tigre.music.player.core.presentation.queue.resources.*
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.tools.analytics.music.MusicEventAnalytics
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.presentation.ScreenContentState.Content
import by.tigre.media.platform.presentation.ScreenContentStateDelegate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

interface CurrentQueueComponent {

    val screenState: StateFlow<ScreenContentState<NowPlayingScreenModel>>
    val scrollToPlayingTrackEvents: SharedFlow<Int>
    val saveDialogState: StateFlow<SaveDialogState?>
    val nameError: StateFlow<Boolean>

    fun retry()
    fun onSongClicked(entry: NowPlayingQueueEntry)
    fun onOverlayReturnToQueueClicked()
    fun onOverlayRowClicked()
    fun onAddToQueueClicked()
    fun onOpenArtistClicked(entry: NowPlayingQueueEntry)
    fun onOpenAlbumClicked(entry: NowPlayingQueueEntry)
    fun onAddToPlaylistClicked(entry: SongInQueueItem)
    fun onMoveTrackUp(entryId: Long)
    fun onMoveTrackDown(entryId: Long)
    fun onMoveTrackToTop(entryId: Long)
    fun onMoveTrackToBottom(entryId: Long)
    fun onTracksReordered(entryIdsInOrder: List<Long>)
    fun onRemoveTrack(entry: NowPlayingQueueEntry)
    fun onSaveClicked()
    fun onSaveNewPlaylistConfirmed(name: String)
    fun dismissSaveDialog()

    data class SaveDialogState(
        val defaultName: String,
    )

    class Impl(
        context: BaseComponentContext,
        dependency: CurrentQueueDependency,
        private val navigator: QueueNavigator
    ) : CurrentQueueComponent, BaseComponentContext by context {

        private val playbackController: PlaybackController = dependency.playbackController
        private val playlistRepository: PlaylistRepository = dependency.playlistRepository
        private val addToPlaylistCoordinator: AddToPlaylistCoordinator = dependency.addToPlaylistCoordinator
        private val eventAnalytics: MusicEventAnalytics = dependency.eventAnalytics

        private val _scrollToPlayingTrackEvents = MutableSharedFlow<Int>(extraBufferCapacity = 1)
        override val scrollToPlayingTrackEvents = _scrollToPlayingTrackEvents.asSharedFlow()

        private val _saveDialogState = MutableStateFlow<SaveDialogState?>(null)
        override val saveDialogState: StateFlow<SaveDialogState?> = _saveDialogState.asStateFlow()

        private val _nameError = MutableStateFlow(false)
        override val nameError: StateFlow<Boolean> = _nameError.asStateFlow()

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = {
                combine(
                    playbackController.currentQueue,
                    playbackController.nowPlayingOverlay,
                    playbackController.interruption,
                    playbackController.isPlaying,
                    playbackController.queueSession,
                ) { queue, overlay, interruption, isPlaying, session ->
                    NowPlayingScreenModel(
                        session = session,
                        overlay = overlay?.let {
                            OverlayQueueEntry(item = it, isPlaying = isPlaying)
                        },
                        queue = queue.map { item ->
                            NowPlayingQueueEntry(
                                id = item.id,
                                song = item.song,
                                isPlaying = item.isPlaying,
                                isInterruptedActive = interruption?.resumePoint?.queueEntryId == item.id,
                                interruptedPositionMs = if (interruption?.resumePoint?.queueEntryId == item.id) {
                                    interruption.resumePoint.positionMs
                                } else {
                                    null
                                },
                            )
                        },
                    )
                }
            },
            mapDataToState = { model -> Content(model) }
        )

        override val screenState: StateFlow<ScreenContentState<NowPlayingScreenModel>> = stateDelegate.screenState

        init {
            launch {
                var previousPlayingIndex: Int? = null
                combine(
                    playbackController.currentQueue,
                    playbackController.nowPlayingOverlay,
                ) { queue, overlay ->
                    if (overlay != null) {
                        null
                    } else {
                        queue.indexOfFirst { it.isPlaying }.takeIf { it >= 0 }
                    }
                }
                    .distinctUntilChanged()
                    .collect { queueIndex ->
                        if (queueIndex != null &&
                            previousPlayingIndex != null &&
                            queueIndex != previousPlayingIndex
                        ) {
                            _scrollToPlayingTrackEvents.emit(queueIndex)
                        }
                        if (queueIndex != null) {
                            previousPlayingIndex = queueIndex
                        }
                    }
            }
        }

        override fun retry() {
            stateDelegate.reload()
        }

        override fun onSongClicked(entry: NowPlayingQueueEntry) {
            eventAnalytics.trackEvent(MusicEvents.Action.QueueSongSelected)
            if (playbackController.nowPlayingOverlay.value != null) {
                eventAnalytics.trackEvent(
                    MusicEvents.Action.ExternalAudioOverlayEnded(MusicEvents.OverlayEndReason.PlayCatalog)
                )
            }
            playbackController.playSongInQueue(entry.id)
        }

        override fun onOverlayReturnToQueueClicked() {
            eventAnalytics.trackEvent(
                MusicEvents.Action.ExternalAudioOverlayEnded(MusicEvents.OverlayEndReason.ReturnButton)
            )
            playbackController.resumeInterruptedSession()
        }

        override fun onOverlayRowClicked() {
            if (playbackController.isPlaying.value) {
                playbackController.pause()
            } else {
                playbackController.resume()
            }
        }

        override fun onAddToQueueClicked() {
            navigator.onOpenCatalog()
        }

        override fun onOpenArtistClicked(entry: NowPlayingQueueEntry) {
            eventAnalytics.trackEvent(MusicEvents.Action.QueueOpenArtist)
            navigator.onOpenArtist(entry.song.artistId)
        }

        override fun onOpenAlbumClicked(entry: NowPlayingQueueEntry) {
            eventAnalytics.trackEvent(MusicEvents.Action.QueueOpenAlbum)
            navigator.onOpenAlbum(entry.song.artistId, entry.song.albumId)
        }

        override fun onAddToPlaylistClicked(entry: SongInQueueItem) {
            launch {
                addToPlaylistCoordinator.show(
                    AddToPlaylistRequest(
                        songIds = listOf(entry.song.id),
                        previewText = getString(Res.string.queue_add_tracks_to_playlist_count, 1),
                    )
                )
            }
        }

        override fun onMoveTrackUp(entryId: Long) {
            moveTrack(entryId = entryId, offset = -1)
        }

        override fun onMoveTrackDown(entryId: Long) {
            moveTrack(entryId = entryId, offset = 1)
        }

        override fun onMoveTrackToTop(entryId: Long) {
            moveTrackToIndex(entryId = entryId, targetIndex = 0)
        }

        override fun onMoveTrackToBottom(entryId: Long) {
            val tracks = currentQueueEntries()
            if (tracks.isEmpty()) return
            moveTrackToIndex(entryId = entryId, targetIndex = tracks.lastIndex)
        }

        override fun onTracksReordered(entryIdsInOrder: List<Long>) {
            val currentOrder = currentQueueEntries().map { it.id }
            if (entryIdsInOrder == currentOrder) return
            playbackController.reorderQueue(entryIdsInOrder)
        }

        override fun onRemoveTrack(entry: NowPlayingQueueEntry) {
            playbackController.removeFromQueue(listOf(entry.id))
        }

        override fun onSaveClicked() {
            launch {
                when (val session = playbackController.queueSession.value) {
                    is QueueSession.FromPlaylist -> {
                        if (!session.isDirty) return@launch
                        val songIds = playbackController.queueSongIdsInListOrder()
                        playlistRepository.replaceTracks(session.playlistId, songIds)
                        playbackController.markPlaylistSaved()
                    }

                    QueueSession.Plain -> {
                        if (playbackController.currentQueue.first().isEmpty()) return@launch
                        _nameError.value = false
                        _saveDialogState.value = SaveDialogState(
                            defaultName = getString(
                                Res.string.queue_save_playlist_default_name,
                                currentDateForPlaylistName(),
                            )
                        )
                    }
                }
            }
        }

        override fun onSaveNewPlaylistConfirmed(name: String) {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) return
            launch {
                if (playlistRepository.isNameTaken(trimmedName)) {
                    _nameError.value = true
                    return@launch
                }
                val songIds = playbackController.queueSongIdsInListOrder()
                val playlistId = playlistRepository.createPlaylist(trimmedName)
                playlistRepository.replaceTracks(playlistId, songIds)
                playbackController.activatePlaylistSession(playlistId, trimmedName)
                _nameError.value = false
                _saveDialogState.value = null
            }
        }

        override fun dismissSaveDialog() {
            _saveDialogState.value = null
            _nameError.value = false
        }

        private fun currentQueueEntries(): List<NowPlayingQueueEntry> =
            (screenState.value as? Content)?.value?.queue.orEmpty()

        private fun moveTrack(entryId: Long, offset: Int) {
            val tracks = currentQueueEntries()
            if (tracks.size < 2) return

            val sourceIndex = tracks.indexOfFirst { it.id == entryId }
            if (sourceIndex == -1) return

            val targetIndex = sourceIndex + offset
            if (targetIndex !in tracks.indices) return

            moveTrackToIndex(entryId = entryId, targetIndex = targetIndex)
        }

        private fun moveTrackToIndex(entryId: Long, targetIndex: Int) {
            val tracks = currentQueueEntries()
            if (tracks.size < 2) return

            val sourceIndex = tracks.indexOfFirst { it.id == entryId }
            if (sourceIndex == -1 || sourceIndex == targetIndex) return

            val reorderedTracks = tracks.toMutableList().apply {
                add(targetIndex, removeAt(sourceIndex))
            }
            playbackController.reorderQueue(reorderedTracks.map { it.id })
        }
    }
}
