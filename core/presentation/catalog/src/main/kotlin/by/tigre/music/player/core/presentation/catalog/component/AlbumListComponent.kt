package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.presentation.catalog.entiry.Album
import by.tigre.music.player.core.presentation.catalog.entiry.Artist
import by.tigre.music.player.core.presentation.catalog.navigation.CatalogNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.presentation.base.ScreenContentState.Content
import by.tigre.music.player.presentation.base.ScreenContentState.Error
import by.tigre.music.player.presentation.base.ScreenContentStateDelegate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlin.random.Random

interface AlbumListComponent {

    val screenState: StateFlow<ScreenContentState<List<Album>>>

    fun retry()
    fun onAlbumClicked(album: Album)

    class Impl(
        context: BaseComponentContext,
        private val navigator: CatalogNavigator,
        artist: Artist
    ) : AlbumListComponent, BaseComponentContext by context {


        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = {
                flowOf(if (Random.nextBoolean()) {
                    Result.success(
                        (1..100).map { Album("$it", "Album $it") }
                    )
                } else {
                    Result.failure(Throwable("test"))
                }).onEach { delay(3000) }
            },
            mapDataToState = { result ->
                result.fold(
                    onSuccess = { Content(it) },
                    onFailure = { Error }
                )
            }
        )

        override val screenState: StateFlow<ScreenContentState<List<Album>>> = stateDelegate.screenState

        override fun retry() {
            stateDelegate.reload()
        }

        override fun onAlbumClicked(album: Album) {
            navigator.showSongs(album)
        }
    }
}
