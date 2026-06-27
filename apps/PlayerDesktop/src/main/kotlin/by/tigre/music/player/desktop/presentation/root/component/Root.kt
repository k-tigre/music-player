package by.tigre.music.player.desktop.presentation.root.component

import by.tigre.media.platform.player.component.EqualizerComponent
import by.tigre.media.platform.player.component.PlayerComponent
import by.tigre.music.player.core.presentation.catalog.component.RootCatalogComponent
import by.tigre.media.platform.player.component.SmallPlayerComponent
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.media.platform.player.di.PlayerComponentProvider
import by.tigre.media.platform.player.navigation.PlayerNavigator
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.playlist.Playlist
import by.tigre.music.player.core.presentation.playlist.current.component.CurrentQueueComponent
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueComponentProvider
import by.tigre.music.player.core.presentation.playlist.current.navigation.QueueNavigator
import by.tigre.music.player.core.presentation.favorites.component.FavoritesComponent
import by.tigre.music.player.core.presentation.favorites.di.FavoritesComponentProvider
import by.tigre.music.player.core.presentation.favorites.navigation.FavoritesNavigator
import by.tigre.music.player.core.presentation.playlist.library.component.RootPlaylistsComponent
import by.tigre.music.player.core.presentation.playlist.library.di.PlaylistsComponentProvider
import by.tigre.music.player.core.presentation.playlist.library.navigation.PlaylistsNavigator
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.presentation.appChildContext
import by.tigre.media.platform.presentation.appChildStack
import by.tigre.media.platform.presentation.trackScreens
import by.tigre.media.platform.tools.analytics.music.MusicAnalyticsDependency
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File

interface Root {

    val playerComponent: SmallPlayerComponent

    val pages: Value<ChildStack<*, PageComponentChild>>
    val mainComponent: Value<ChildStack<*, MainComponentChild>>
    val isScanning: StateFlow<Boolean>

    fun selectPage(index: Int)
    fun openPlaylistDetail(id: Playlist.Id)
    fun addCatalogFolder(folder: File)

    /** Stand-alone equalizer for the desktop EQ window (not the main navigation stack). */
    fun createEqualizerComponent(onClose: () -> Unit): EqualizerComponent

    sealed interface PageComponentChild {
        class Queue(val component: CurrentQueueComponent) : PageComponentChild
        class Catalog(val component: RootCatalogComponent) : PageComponentChild
        class Playlists(val component: RootPlaylistsComponent) : PageComponentChild
        class Favorites(val component: FavoritesComponent) : PageComponentChild
    }

    sealed interface MainComponentChild {
        data object Main : MainComponentChild
        class Player(val component: PlayerComponent) : MainComponentChild
        class Equalizer(val component: EqualizerComponent) : MainComponentChild
    }

    class Impl(
        context: BaseComponentContext,
        dependency: MusicAnalyticsDependency,
        catalogComponentProvider: CatalogComponentProvider,
        private val playerComponentProvider: PlayerComponentProvider,
        currentQueueComponent: CurrentQueueComponentProvider,
        playlistsComponentProvider: PlaylistsComponentProvider,
        favoritesComponentProvider: FavoritesComponentProvider,
        private val onAddFolder: suspend (File) -> Unit,
    ) : Root, BaseComponentContext by context {

        private val eventAnalytics = dependency.eventAnalytics
        private val screenAnalytics = dependency.screenAnalytics

        private val pagesNavigation = StackNavigation<PagesConfig>()
        private val mainNavigation = StackNavigation<MainConfig>()
        private var catalogReturnPageIndex: Int? = null

        private val _isScanning = MutableStateFlow(false)
        override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

        @OptIn(DelicateDecomposeApi::class)
        private val playerNavigator = object : PlayerNavigator {
            override fun showQueue() {
                mainNavigation.pop()
            }

            override fun playerView() {
                mainNavigation.push(MainConfig.Player)
            }

            override fun showEqualizer() {
                mainNavigation.push(MainConfig.Equalizer)
            }

            override fun closeEqualizer() {
                mainNavigation.pop()
            }

            override fun showSettings() = Unit

            override fun closeSettings() = Unit
        }

        override val playerComponent: SmallPlayerComponent by lazy {
            playerComponentProvider.createSmallPlayerComponent(
                context = appChildContext("player"),
                navigator = playerNavigator
            )
        }

        private val catalogComponent: RootCatalogComponent =
            catalogComponentProvider.createRootCatalogComponent(
                context = appChildContext("catalog"),
                onExitRequested = {
                    catalogReturnPageIndex?.let(::selectPage)
                    catalogReturnPageIndex = null
                },
            )

        private val queueNavigator = object : QueueNavigator {
            override fun onOpenCatalog() = selectPage(3)

            override fun onOpenArtist(artistId: Artist.Id) {
                selectPage(3)
                catalogComponent.navigateToArtist(artistId)
            }

            override fun onOpenAlbum(artistId: Artist.Id, albumId: Album.Id) {
                selectPage(3)
                catalogComponent.navigateToAlbum(artistId, albumId)
            }
        }

        private val playlistsNavigator = object : PlaylistsNavigator {
            override fun openDetail(id: Playlist.Id) = Unit

            override fun showPreviousScreen() = Unit

            override fun openCatalog() = selectPage(3)

            override fun openQueue() = selectPage(0)

            override fun openArtist(id: Artist.Id) {
                selectPage(3)
                catalogComponent.navigateToArtist(id)
            }

            override fun openAlbum(artistId: Artist.Id, albumId: Album.Id) {
                selectPage(3)
                catalogComponent.navigateToAlbum(artistId, albumId)
            }
        }

        private val favoritesNavigator = object : FavoritesNavigator {
            override fun openCatalog() = selectPage(3)

            override fun openArtist(id: Artist.Id) {
                openCatalogFromFavorites {
                    catalogComponent.navigateToArtist(id, returnOnRootBack = true)
                }
            }

            override fun openAlbum(artistId: Artist.Id, albumId: Album.Id) {
                openCatalogFromFavorites {
                    catalogComponent.navigateToAlbum(artistId, albumId, returnOnRootBack = true)
                }
            }
        }

        private val playlistsComponent: RootPlaylistsComponent =
            playlistsComponentProvider.createRootPlaylistsComponent(
                context = appChildContext("playlists"),
                navigator = playlistsNavigator,
            )

        private val favoritesComponent: FavoritesComponent =
            favoritesComponentProvider.createFavoritesComponent(
                context = appChildContext("favorites"),
                navigator = favoritesNavigator,
            )

        override val pages: Value<ChildStack<*, PageComponentChild>> =
            appChildStack(
                source = pagesNavigation,
                initialStack = { listOf(PagesConfig.Catalog) },
                key = "pages",
                handleBackButton = false
            ) { config, componentContext ->
                when (config) {
                    PagesConfig.Catalog -> PageComponentChild.Catalog(catalogComponent)
                    PagesConfig.Playlists -> PageComponentChild.Playlists(playlistsComponent)
                    PagesConfig.Favorites -> PageComponentChild.Favorites(favoritesComponent)

                    PagesConfig.Queue -> PageComponentChild.Queue(
                        currentQueueComponent.createCurrentQueueComponent(
                            componentContext,
                            navigator = queueNavigator
                        )
                    )
                }
            }

        override val mainComponent: Value<ChildStack<*, MainComponentChild>> =
            appChildStack(
                source = mainNavigation,
                initialStack = { listOf(MainConfig.Main) },
                key = "main",
                handleBackButton = true
            ) { config, componentContext ->
                when (config) {
                    MainConfig.Main -> MainComponentChild.Main

                    MainConfig.Player -> MainComponentChild.Player(
                        playerComponentProvider.createPlayerComponent(
                            context = componentContext,
                            navigator = playerNavigator
                        )
                    )

                    MainConfig.Equalizer -> MainComponentChild.Equalizer(
                        playerComponentProvider.createEqualizerComponent(
                            onClose = playerNavigator::closeEqualizer
                        )
                    )
                }
            }

        override fun selectPage(index: Int) {
            if (index != 3) {
                catalogReturnPageIndex = null
                catalogComponent.clearReturnOnRootBack()
            }
            when (index) {
                0 -> pagesNavigation.bringToFront(PagesConfig.Queue)
                1 -> {
                    if (pages.value.active.configuration != PagesConfig.Playlists) {
                        eventAnalytics.trackEvent(MusicEvents.Action.NavOpenPlaylists)
                    }
                    pagesNavigation.bringToFront(PagesConfig.Playlists)
                }
                2 -> {
                    if (pages.value.active.configuration != PagesConfig.Favorites) {
                        eventAnalytics.trackEvent(MusicEvents.Action.NavOpenFavorites)
                    }
                    pagesNavigation.bringToFront(PagesConfig.Favorites)
                }
                3 -> {
                    catalogReturnPageIndex = null
                    catalogComponent.navigateToRoot()
                    pagesNavigation.bringToFront(PagesConfig.Catalog)
                }
            }
        }

        override fun openPlaylistDetail(id: Playlist.Id) {
            selectPage(1)
            playlistsComponent.openDetail(id)
        }

        override fun addCatalogFolder(folder: File) {
            launch {
                _isScanning.value = true
                try {
                    onAddFolder(folder)
                } finally {
                    _isScanning.value = false
                }
            }
        }

        override fun createEqualizerComponent(onClose: () -> Unit): EqualizerComponent =
            playerComponentProvider.createEqualizerComponent(onClose)

        private fun openCatalogFromFavorites(navigate: () -> Unit) {
            catalogReturnPageIndex = 2
            pagesNavigation.bringToFront(PagesConfig.Catalog)
            navigate()
        }

        init {
            launch {
                pages.trackScreens<PagesConfig, MusicEvents.Screen>(
                    trackScreen = screenAnalytics::trackScreen,
                    name = "PagesConfig",
                ) {
                    when (it) {
                        PagesConfig.Queue -> MusicEvents.Screen.Queue
                        PagesConfig.Catalog -> MusicEvents.Screen.CatalogTab
                        PagesConfig.Playlists -> MusicEvents.Screen.PlaylistsList
                        PagesConfig.Favorites -> MusicEvents.Screen.FavoritesTab
                    }
                }
            }
        }

        @Serializable
        private sealed interface PagesConfig {
            @Serializable
            data object Queue : PagesConfig

            @Serializable
            data object Catalog : PagesConfig

            @Serializable
            data object Playlists : PagesConfig

            @Serializable
            data object Favorites : PagesConfig
        }

        @Serializable
        private sealed interface MainConfig {
            @Serializable
            data object Main : MainConfig

            @Serializable
            data object Player : MainConfig

            @Serializable
            data object Equalizer : MainConfig
        }
    }
}
