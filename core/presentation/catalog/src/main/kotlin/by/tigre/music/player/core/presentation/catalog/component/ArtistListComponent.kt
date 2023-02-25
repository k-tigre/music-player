package by.tigre.music.player.core.presentation.catalog.component

import android.util.Log
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.music.player.core.presentation.catalog.entiry.Artist
import by.tigre.music.player.core.presentation.catalog.navigation.CatalogNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.presentation.base.ScreenContentStateDelegate
import by.tigre.music.player.tools.coroutines.extensions.log
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

interface ArtistListComponent {

    val screenState: StateFlow<ScreenContentState<List<Artist>>>

    fun retry()
    fun onArtistClicked(artist: Artist)

    class Impl(
        context: BaseComponentContext,
        dependency: CatalogDependency,
        private val navigator: CatalogNavigator
    ) : ArtistListComponent, BaseComponentContext by context {

        private val catalogSource: CatalogSource = dependency.catalogSource

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = {

                flow { emit(catalogSource.getArtists()) }
                    .log("catalogSource.getArtists")
//                flowOf(if (Random.nextBoolean()) {
//                    Result.success(
//                        (1..100).map { Artist("$it", "Artist $it") }
//                    )
//                } else {
//                    Result.failure(Throwable("test"))
//                }).onEach { delay(3000) }
            },
            mapDataToState = { artists ->
                ScreenContentState.Content(
                    artists
                        .map { artist ->
                            Artist(id = artist.id, name = artist.name, albums = artist.albumCount, songs = artist.songCount)
                        }
                )
//                result.fold(
//                    onSuccess = { Content(it) },
//                    onFailure = { Error }
//                )
            }
        )

        override val screenState: StateFlow<ScreenContentState<List<Artist>>> = stateDelegate.screenState

        override fun retry() {
            stateDelegate.reload()
        }

        override fun onArtistClicked(artist: Artist) {
            navigator.showShowAlbums(artist)
        }
    }
}
