package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.music.player.core.presentation.catalog.navigation.CatalogNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.appChildStack
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

interface RootCatalogComponent {

    val childStack: Value<ChildStack<*, CatalogChild>>

    fun navigateToArtist(artistId: Artist.Id)
    fun navigateToAlbum(artistId: Artist.Id, albumId: Album.Id)

    sealed interface CatalogChild {
        class ArtistsList(val component: ArtistListComponent) : CatalogChild
        class AlbumsList(val component: AlbumListComponent) : CatalogChild
        class SongsList(val component: SongsListComponent) : CatalogChild
    }

    class Impl(
        context: BaseComponentContext,
        private val componentProvider: CatalogComponentProvider,
        private val dependency: CatalogDependency,
    ) : RootCatalogComponent, BaseComponentContext by context {
        private val navigation = StackNavigation<CatalogConfig>()
        private val catalogSource: CatalogSource = dependency.catalogSource

        private val navigator = object : CatalogNavigator {
            override fun showArtists() {
                navigation.push(CatalogConfig.ArtistsList)
            }

            override fun showShowAlbums(artist: Artist) {
                navigation.push(CatalogConfig.AlbumsList(artist))
            }

            override fun showSongs(album: Album, artist: Artist) {
                navigation.push(CatalogConfig.SongsList(album, artist))
            }

            override fun showPreviousScreen() {
                navigation.pop { isSuccess ->
                    if (isSuccess.not()) {
                        // TODO
                    }
                }
            }

            override fun showSongsForTrack(song: Song) {
                launch {
                    val artist = catalogSource.getArtistById(song.artistId) ?: return@launch
                    val album = catalogSource.getAlbumById(song.artistId, song.albumId) ?: return@launch
                    withContext(Dispatchers.Main.immediate) {
                        navigation.push(CatalogConfig.SongsList(album, artist))
                    }
                }
            }
        }

        private val stack = appChildStack(
            source = navigation,
            initialStack = { listOf(CatalogConfig.ArtistsList) },
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
                            album = config.album,
                            artist = config.artist
                        )
                    )
                }
            }

        override val childStack: Value<ChildStack<*, CatalogChild>> = stack

        override fun navigateToArtist(artistId: Artist.Id) {
            launch {
                val artist = catalogSource.getArtistById(artistId) ?: return@launch
                withContext(Dispatchers.Main.immediate) {
                    navigation.replaceAll(
                        CatalogConfig.ArtistsList,
                        CatalogConfig.AlbumsList(artist),
                    )
                }
            }
        }

        override fun navigateToAlbum(artistId: Artist.Id, albumId: Album.Id) {
            launch {
                val artist = catalogSource.getArtistById(artistId) ?: return@launch
                val album = catalogSource.getAlbumById(artistId, albumId) ?: return@launch
                withContext(Dispatchers.Main.immediate) {
                    navigation.replaceAll(
                        CatalogConfig.ArtistsList,
                        CatalogConfig.AlbumsList(artist),
                        CatalogConfig.SongsList(album, artist),
                    )
                }
            }
        }

        @Serializable
        private sealed interface CatalogConfig {

            @Serializable
            data object ArtistsList : CatalogConfig

            @Serializable
            class AlbumsList(val artist: Artist) : CatalogConfig

            @Serializable
            class SongsList(val album: Album, val artist: Artist) : CatalogConfig
        }
    }
}
