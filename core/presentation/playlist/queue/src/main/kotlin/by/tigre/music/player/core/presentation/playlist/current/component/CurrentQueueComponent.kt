package by.tigre.music.player.core.presentation.playlist.current.component

import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.entiry.playback.SongInQueueItem
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueDependency
import by.tigre.music.player.core.presentation.playlist.current.navigation.QueueNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.presentation.base.ScreenContentState.Content
import by.tigre.music.player.presentation.base.ScreenContentStateDelegate
import kotlinx.coroutines.flow.StateFlow

interface CurrentQueueComponent {

    val screenState: StateFlow<ScreenContentState<List<SongInQueueItem>>>
    val title: String

    fun retry()
    fun onSongClicked(song: SongInQueueItem)
    fun onAddToQueueClicked()

    class Impl(
        context: BaseComponentContext,
        dependency: CurrentQueueDependency,
        private val navigator: QueueNavigator
    ) : CurrentQueueComponent, BaseComponentContext by context {

        override val title: String = "Current Queue"

        private val playbackController: PlaybackController = dependency.playbackController

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
            playbackController.playSongInQueue(song.id)
        }

        override fun onAddToQueueClicked() {
            navigator.onOpenCatalog()
        }
    }
}
