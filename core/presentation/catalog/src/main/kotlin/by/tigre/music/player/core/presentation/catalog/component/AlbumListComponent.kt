package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.music.player.core.presentation.catalog.navigation.CatalogNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.presentation.base.ScreenContentState.Content
import by.tigre.music.player.presentation.base.ScreenContentStateDelegate
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

interface AlbumListComponent {

    val screenState: StateFlow<ScreenContentState<List<Album>>>
    val artist: Artist

    fun retry()
    fun onAlbumClicked(album: Album)
    fun onBackClicked()
    fun onPlayAlbumClicked(album: Album)

    class Impl(
        context: BaseComponentContext,
        dependency: CatalogDependency,
        private val navigator: CatalogNavigator,
        override val artist: Artist
    ) : AlbumListComponent, BaseComponentContext by context {

        private val catalogSource: CatalogSource = dependency.catalogSource
        private val playbackController: PlaybackController = dependency.playbackController

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = {
                flow { emit(catalogSource.getAlbums(artist.id)) }
            },
            mapDataToState = { albums ->
                Content(albums)
            }
        )

        override val screenState: StateFlow<ScreenContentState<List<Album>>> = stateDelegate.screenState

        override fun retry() {
            stateDelegate.reload()
        }

        override fun onBackClicked() {
            navigator.showPreviousScreen()
        }

        override fun onAlbumClicked(album: Album) {
            navigator.showSongs(album)
        }

        override fun onPlayAlbumClicked(album: Album) {
            playbackController.playAlbum(album)
        }
    }
}
