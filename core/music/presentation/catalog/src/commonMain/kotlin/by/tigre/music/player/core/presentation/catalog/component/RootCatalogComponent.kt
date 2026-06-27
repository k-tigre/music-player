package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.music.player.core.presentation.catalog.navigation.CatalogNavigator
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.presentation.appChildStack
import by.tigre.media.platform.presentation.trackScreens
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

interface RootCatalogComponent {

    val childStack: Value<ChildStack<*, CatalogChild>>

    fun navigateToArtist(artistId: Artist.Id, returnOnRootBack: Boolean = false)
    fun navigateToAlbum(artistId: Artist.Id, albumId: Album.Id, returnOnRootBack: Boolean = false)
    fun clearReturnOnRootBack()
    fun navigateToRoot()

    sealed interface CatalogChild {
        class ArtistsList(val component: ArtistListComponent) : CatalogChild
        class AlbumsList(val component: AlbumListComponent) : CatalogChild
        class SongsList(val component: SongsListComponent) : CatalogChild
    }

    class Impl(
        context: BaseComponentContext,
        private val componentProvider: CatalogComponentProvider,
        private val dependency: CatalogDependency,
        private val onOpenSettings: (() -> Unit)? = null,
        private val onExitRequested: (() -> Unit)? = null,
    ) : RootCatalogComponent, BaseComponentContext by context {
        private val navigation = StackNavigation<CatalogConfig>()
        private val catalogSource: CatalogSource = dependency.catalogSource
        private var returnOnRootBack = false
        private val externalExitBackCallback = BackCallback(isEnabled = false) {
            returnOnRootBack = false
            onExitRequested?.invoke()
        }

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
                handleBack()
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
                            navigator = navigator,
                            onOpenSettings = onOpenSettings,
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

        init {
            backHandler.register(externalExitBackCallback)
            stack.subscribe {
                externalExitBackCallback.isEnabled = returnOnRootBack && it.backStack.isEmpty()
            }

            launch {
                stack.trackScreens<CatalogConfig, MusicEvents.Screen>(
                    trackScreen = dependency.screenAnalytics::trackScreen,
                    name = "CatalogConfig",
                ) {
                    when (it) {
                        CatalogConfig.ArtistsList -> MusicEvents.Screen.ArtistsList
                        is CatalogConfig.AlbumsList -> MusicEvents.Screen.AlbumsList(it.artist.id.value)
                        is CatalogConfig.SongsList -> MusicEvents.Screen.SongsList(it.album.id.value, it.artist.id.value)
                    }
                }
            }
        }

        override fun navigateToArtist(artistId: Artist.Id, returnOnRootBack: Boolean) {
            launch {
                val artist = catalogSource.getArtistById(artistId) ?: return@launch
                withContext(Dispatchers.Main.immediate) {
                    this@Impl.returnOnRootBack = returnOnRootBack
                    if (returnOnRootBack) {
                        navigation.replaceAll(CatalogConfig.AlbumsList(artist))
                    } else {
                        navigation.replaceAll(
                            CatalogConfig.ArtistsList,
                            CatalogConfig.AlbumsList(artist),
                        )
                    }
                }
            }
        }

        override fun navigateToAlbum(artistId: Artist.Id, albumId: Album.Id, returnOnRootBack: Boolean) {
            launch {
                val artist = catalogSource.getArtistById(artistId) ?: return@launch
                val album = catalogSource.getAlbumById(artistId, albumId) ?: return@launch
                withContext(Dispatchers.Main.immediate) {
                    this@Impl.returnOnRootBack = returnOnRootBack
                    if (returnOnRootBack) {
                        navigation.replaceAll(CatalogConfig.SongsList(album, artist))
                    } else {
                        navigation.replaceAll(
                            CatalogConfig.ArtistsList,
                            CatalogConfig.AlbumsList(artist),
                            CatalogConfig.SongsList(album, artist),
                        )
                    }
                }
            }
        }

        override fun clearReturnOnRootBack() {
            returnOnRootBack = false
            externalExitBackCallback.isEnabled = false
        }

        override fun navigateToRoot() {
            clearReturnOnRootBack()
            navigation.replaceAll(CatalogConfig.ArtistsList)
        }

        private fun handleBack() {
            navigation.pop { isSuccess ->
                if (!isSuccess && returnOnRootBack) {
                    returnOnRootBack = false
                    onExitRequested?.invoke()
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
