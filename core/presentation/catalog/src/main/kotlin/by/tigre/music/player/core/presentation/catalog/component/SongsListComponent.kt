package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.music.player.core.presentation.catalog.entiry.Album
import by.tigre.music.player.core.presentation.catalog.entiry.Song
import by.tigre.music.player.core.presentation.catalog.navigation.CatalogNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.presentation.base.ScreenContentState.Content
import by.tigre.music.player.presentation.base.ScreenContentStateDelegate
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

interface SongsListComponent {

    val screenState: StateFlow<ScreenContentState<List<Song>>>
    val album: Album

    fun retry()
    fun onSongClicked(song: Song)

    class Impl(
        context: BaseComponentContext,
        dependency: CatalogDependency,
        private val navigator: CatalogNavigator,
        override val album: Album
    ) : SongsListComponent, BaseComponentContext by context {

        private val catalogSource: CatalogSource = dependency.catalogSource

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = {
                flow { emit(catalogSource.getSongsByAlbum(albumId = album.id)) }
            },
            mapDataToState = { songs ->
                Content(songs.map { song ->
                    Song(
                        id = song.id,
                        name = song.name
                    )
                })
            }
        )

        override val screenState: StateFlow<ScreenContentState<List<Song>>> = stateDelegate.screenState

        override fun retry() {
            stateDelegate.reload()
        }

        override fun onSongClicked(song: Song) {

        }
    }
}
