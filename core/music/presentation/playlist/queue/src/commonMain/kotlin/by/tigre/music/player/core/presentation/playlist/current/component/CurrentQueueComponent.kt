package by.tigre.music.player.core.presentation.playlist.current.component

import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.entiry.playback.SongInQueueItem
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueDependency
import by.tigre.music.player.core.presentation.playlist.current.navigation.QueueNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.tools.analytics.music.MusicEventAnalytics
import by.tigre.music.player.tools.analytics.music.MusicEvents
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.presentation.base.ScreenContentState.Content
import by.tigre.music.player.presentation.base.ScreenContentStateDelegate
import kotlinx.coroutines.flow.StateFlow

interface CurrentQueueComponent {

    val screenState: StateFlow<ScreenContentState<List<SongInQueueItem>>>

    fun retry()
    fun onSongClicked(song: SongInQueueItem)
    fun onAddToQueueClicked()
    fun onOpenArtistClicked(song: SongInQueueItem)
    fun onOpenAlbumClicked(song: SongInQueueItem)

    class Impl(
        context: BaseComponentContext,
        dependency: CurrentQueueDependency,
        private val navigator: QueueNavigator
    ) : CurrentQueueComponent, BaseComponentContext by context {

        private val playbackController: PlaybackController = dependency.playbackController
        private val eventAnalytics: MusicEventAnalytics = dependency.eventAnalytics

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = {
                playbackController.currentQueue
            },
            mapDataToState = { songs -> Content(songs) }
        )

        override val screenState: StateFlow<ScreenContentState<List<SongInQueueItem>>> = stateDelegate.screenState

        override fun retry() {
            stateDelegate.reload()
        }

        override fun onSongClicked(song: SongInQueueItem) {
            eventAnalytics.trackEvent(MusicEvents.Action.QueueSongSelected)
            playbackController.playSongInQueue(song.id)
        }

        override fun onAddToQueueClicked() {
            eventAnalytics.trackEvent(MusicEvents.Action.NavOpenCatalog)
            navigator.onOpenCatalog()
        }

        override fun onOpenArtistClicked(song: SongInQueueItem) {
            eventAnalytics.trackEvent(MusicEvents.Action.QueueOpenArtist)
            navigator.onOpenArtist(song.song.artistId)
        }

        override fun onOpenAlbumClicked(song: SongInQueueItem) {
            eventAnalytics.trackEvent(MusicEvents.Action.QueueOpenAlbum)
            navigator.onOpenAlbum(song.song.artistId, song.song.albumId)
        }
    }
}
