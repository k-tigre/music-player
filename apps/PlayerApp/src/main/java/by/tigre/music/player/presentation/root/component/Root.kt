package by.tigre.music.player.presentation.root.component

import by.tigre.music.player.core.presentation.catalog.component.EqualizerComponent
import by.tigre.music.player.core.presentation.catalog.component.PlayerComponent
import by.tigre.music.player.core.presentation.catalog.component.RootCatalogComponent
import by.tigre.music.player.core.presentation.catalog.component.SmallPlayerComponent
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.PlayerComponentProvider
import by.tigre.music.player.core.presentation.catalog.navigation.PlayerNavigator
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.presentation.playlist.current.component.CurrentQueueComponent
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueComponentProvider
import by.tigre.music.player.core.presentation.playlist.current.navigation.QueueNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.appChildContext
import by.tigre.music.player.presentation.base.appChildStack
import by.tigre.music.player.presentation.base.trackScreens
import by.tigre.music.player.tools.analytics.Event
import by.tigre.music.player.tools.analytics.EventAnalytics
import by.tigre.music.player.tools.analytics.ScreenAnalytics
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface Root {

    val playerComponent: SmallPlayerComponent

    val onStartServiceEvent: Flow<Unit>

    val pages: Value<ChildStack<*, PageComponentChild>>
    val mainComponent: Value<ChildStack<*, MainComponentChild>>

    fun selectPage(index: Int)

    sealed interface PageComponentChild {
        class Queue(val component: CurrentQueueComponent) : PageComponentChild
        class Catalog(val component: RootCatalogComponent) : PageComponentChild
    }

    sealed interface MainComponentChild {
        data object Main : MainComponentChild
        class Player(val component: PlayerComponent) : MainComponentChild
        class Equalizer(val component: EqualizerComponent) : MainComponentChild
    }

    class Impl(
        context: BaseComponentContext,
        catalogComponentProvider: CatalogComponentProvider,
        playerComponentProvider: PlayerComponentProvider,
        currentQueueComponent: CurrentQueueComponentProvider,
        screenAnalytics: ScreenAnalytics,
        private val eventAnalytics: EventAnalytics,
    ) : Root, BaseComponentContext by context {

        private val pagesNavigation = StackNavigation<PagesConfig>()
        private val mainNavigation = StackNavigation<MainConfig>()

        private val playerNavigator = object : PlayerNavigator {
            override fun showQueue() {
                eventAnalytics.trackEvent(Event.Action.UI.Button.OpenQueue)
                mainNavigation.pop()
            }

            override fun playerView() {
                eventAnalytics.trackEvent(Event.Action.UI.Button.OpenPlayer)
                mainNavigation.push(MainConfig.Player)
            }

            override fun showEqualizer() {
                eventAnalytics.trackEvent(Event.Action.UI.Button.OpenEqualizer)
                mainNavigation.push(MainConfig.Equalizer)
            }

            override fun closeEqualizer() {
                mainNavigation.pop()
            }
        }

        override val playerComponent: SmallPlayerComponent by lazy {
            playerComponentProvider.createSmallPlayerComponent(
                context = appChildContext("player"),
                navigator = playerNavigator
            )
        }

        override val onStartServiceEvent = MutableSharedFlow<Unit>()

        private val catalogComponent: RootCatalogComponent =
            catalogComponentProvider.createRootCatalogComponent(appChildContext("catalog"))

        private val queueNavigator = object : QueueNavigator {
            override fun onOpenCatalog() = selectPage(1)

            override fun onOpenArtist(artistId: Artist.Id) {
                selectPage(1)
                catalogComponent.navigateToArtist(artistId)
            }

            override fun onOpenAlbum(artistId: Artist.Id, albumId: Album.Id) {
                selectPage(1)
                catalogComponent.navigateToAlbum(artistId, albumId)
            }
        }

        override val pages: Value<ChildStack<*, PageComponentChild>> =
            appChildStack(
                source = pagesNavigation,
                initialStack = { listOf(PagesConfig.Queue) },
                key = "pages",
                handleBackButton = false
            ) { config, componentContext ->
                when (config) {
                    PagesConfig.Catalog -> PageComponentChild.Catalog(catalogComponent)

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
            when (index) {
                0 -> pagesNavigation.bringToFront(PagesConfig.Queue)
                1 -> {
                    eventAnalytics.trackEvent(Event.Action.UI.Button.OpenCatalog)
                    pagesNavigation.bringToFront(PagesConfig.Catalog)
                }
            }
        }

        init {
            launch {
                pages.trackScreens<PagesConfig>(screenAnalytics, "PagesConfig") {
                    when (it) {
                        PagesConfig.Queue -> Event.Screen.Queue
                        PagesConfig.Catalog -> Event.Screen.CatalogTab
                    }
                }
            }
            launch {
                mainComponent.trackScreens<MainConfig>(screenAnalytics, "MainConfig") {
                    when (it) {
                        MainConfig.Main -> Event.Screen.RootOverlay
                        MainConfig.Player -> Event.Screen.Player
                        MainConfig.Equalizer -> Event.Screen.Equalizer
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
