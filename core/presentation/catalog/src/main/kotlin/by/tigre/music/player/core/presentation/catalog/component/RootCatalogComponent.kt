package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.entiry.Album
import by.tigre.music.player.core.presentation.catalog.entiry.Artist
import by.tigre.music.player.core.presentation.catalog.navigation.CatalogNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.appChildStack
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import kotlinx.parcelize.Parcelize

interface RootCatalogComponent {

    val childStack: Value<ChildStack<*, CatalogChild>>

    sealed interface CatalogChild {
        class CatalogFolder(val component: CatalogFolderSelectorComponent) : CatalogChild
        class ArtistsList(val component: ArtistListComponent) : CatalogChild
        class AlbumsList(val component: AlbumListComponent) : CatalogChild
        class SongsList(val component: SongsListComponent) : CatalogChild
    }

    class Impl(
        context: BaseComponentContext,
        private val componentProvider: CatalogComponentProvider
    ) : RootCatalogComponent, BaseComponentContext by context {
        private val navigation = StackNavigation<CatalogConfig>()

        private val navigator = object : CatalogNavigator {
            override fun showArtists() {
                navigation.push(CatalogConfig.ArtistsList)
            }

            override fun showShowAlbums(artist: Artist) {
                navigation.push(CatalogConfig.AlbumsList(artist))
            }

            override fun showSongs(album: Album) {
                navigation.push(CatalogConfig.SongsList(album))
            }

            override fun showPreviousScreen() {
                navigation.pop { isSuccess ->
                    if (isSuccess.not()) {
                        // TODO
                    }
                }
            }
        }

        private val stack = appChildStack(
            source = navigation,
            initialStack = { listOf(CatalogConfig.CatalogFolder) },
            childFactory = ::child,
            handleBackButton = true
        )

        private fun child(
            config: CatalogConfig,
            context: BaseComponentContext
        ): CatalogChild =
            when (config) {
                is CatalogConfig.ArtistsList -> {
                    CatalogChild.ArtistsList(
                        componentProvider.createArtistListComponent(
                            context = context,
                            navigator = navigator
                        )
                    )
                }
                is CatalogConfig.AlbumsList -> {
                    CatalogChild.AlbumsList(
                        componentProvider.createAlbumListComponent(
                            context = context,
                            navigator = navigator,
                            artist = config.artist
                        )
                    )
                }
                is CatalogConfig.SongsList -> {
                    CatalogChild.SongsList(
                        componentProvider.createSongsListComponent(
                            context = context,
                            navigator = navigator,
                            album = config.album
                        )
                    )
                }

                is CatalogConfig.CatalogFolder -> {
                    CatalogChild.CatalogFolder(
                        componentProvider.createCatalogFolderSelectorComponent(
                            context = context,
                            navigator = navigator
                        )
                    )
                }
            }

        override val childStack: Value<ChildStack<*, CatalogChild>> = stack

        private sealed interface CatalogConfig : Parcelable {

            @Parcelize
            object CatalogFolder : CatalogConfig

            @Parcelize
            object ArtistsList : CatalogConfig

            @Parcelize
            class AlbumsList(val artist: Artist) : CatalogConfig

            @Parcelize
            class SongsList(val album: Album) : CatalogConfig
        }
    }
}
