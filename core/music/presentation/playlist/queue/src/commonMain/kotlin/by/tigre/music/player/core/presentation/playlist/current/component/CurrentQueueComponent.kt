package by.tigre.music.player.core.presentation.playlist.current.component

import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.data.playlist.AddToPlaylistCoordinator
import by.tigre.music.player.core.data.playlist.AddToPlaylistRequest
import by.tigre.music.player.core.entiry.playback.NowPlayingQueueEntry
import by.tigre.music.player.core.entiry.playback.NowPlayingScreenModel
import by.tigre.music.player.core.entiry.playback.OverlayQueueEntry
import by.tigre.music.player.core.entiry.playback.SongInQueueItem
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueDependency
import by.tigre.music.player.core.presentation.playlist.current.navigation.QueueNavigator
import `by`.tigre.music.player.core.presentation.queue.resources.Res
import `by`.tigre.music.player.core.presentation.queue.resources.*
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.tools.analytics.music.MusicEventAnalytics
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.presentation.ScreenContentState.Content
import by.tigre.media.platform.presentation.ScreenContentStateDelegate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

interface CurrentQueueComponent {

    val screenState: StateFlow<ScreenContentState<NowPlayingScreenModel>>
    val scrollToPlayingTrackEvents: SharedFlow<Int>

    fun retry()
    fun onSongClicked(entry: NowPlayingQueueEntry)
    fun onOverlayReturnToQueueClicked()
    fun onOverlayRowClicked()
    fun onAddToQueueClicked()
    fun onOpenArtistClicked(entry: NowPlayingQueueEntry)
    fun onOpenAlbumClicked(entry: NowPlayingQueueEntry)
    fun onAddToPlaylistClicked(entry: SongInQueueItem)

    class Impl(
        context: BaseComponentContext,
        dependency: CurrentQueueDependency,
        private val navigator: QueueNavigator
    ) : CurrentQueueComponent, BaseComponentContext by context {

        private val playbackController: PlaybackController = dependency.playbackController
        private val addToPlaylistCoordinator: AddToPlaylistCoordinator = dependency.addToPlaylistCoordinator
        private val eventAnalytics: MusicEventAnalytics = dependency.eventAnalytics

        private val _scrollToPlayingTrackEvents = MutableSharedFlow<Int>(extraBufferCapacity = 1)
        override val scrollToPlayingTrackEvents = _scrollToPlayingTrackEvents.asSharedFlow()

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = {
                combine(
                    playbackController.currentQueue,
                    playbackController.nowPlayingOverlay,
                    playbackController.interruption,
                    playbackController.isPlaying,
                ) { queue, overlay, interruption, isPlaying ->
                    NowPlayingScreenModel(
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
    }
}
