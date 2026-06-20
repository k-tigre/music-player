package by.tigre.music.player.presentation.root.component

import by.tigre.media.platform.player.component.EqualizerComponent
import by.tigre.media.platform.player.component.PlayerComponent
import by.tigre.music.player.core.presentation.catalog.component.RootCatalogComponent
import by.tigre.media.platform.player.component.SmallPlayerComponent
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.media.platform.player.di.PlayerComponentProvider
import by.tigre.media.platform.player.navigation.PlayerNavigator
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.presentation.playlist.current.component.CurrentQueueComponent
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueComponentProvider
import by.tigre.music.player.core.presentation.playlist.current.navigation.QueueNavigator
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.presentation.appChildContext
import by.tigre.media.platform.presentation.appChildStack
import by.tigre.media.platform.presentation.trackScreens
import by.tigre.media.platform.tools.analytics.common.CommonEvents
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import by.tigre.music.player.presentation.root.di.RootDependency
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface Root {

    val playerComponent: SmallPlayerComponent

    val onStartServiceEvent: Flow<Unit>

    val showDefaultPlayerPrompt: StateFlow<Boolean>

    val pages: Value<ChildStack<*, PageComponentChild>>
    val mainComponent: Value<ChildStack<*, MainComponentChild>>

    fun selectPage(index: Int)

    fun dismissDefaultPlayerPrompt()

    fun confirmDefaultPlayerPrompt()

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
        dependency: RootDependency,
        catalogComponentProvider: CatalogComponentProvider,
        playerComponentProvider: PlayerComponentProvider,
        currentQueueComponent: CurrentQueueComponentProvider,
    ) : Root, BaseComponentContext by context {

        private val eventAnalytics = dependency.eventAnalytics
        private val screenAnalytics = dependency.screenAnalytics
        private val playerSettings = dependency.playerSettings

        private val showDefaultPlayerPromptState = MutableStateFlow(playerSettings.shouldShowPrompt())
        override val showDefaultPlayerPrompt: StateFlow<Boolean> = showDefaultPlayerPromptState.asStateFlow()

        private val pagesNavigation = StackNavigation<PagesConfig>()
        private val mainNavigation = StackNavigation<MainConfig>()

        private val playerNavigator = object : PlayerNavigator {
            override fun showQueue() {
                eventAnalytics.trackEvent(MusicEvents.Action.NavOpenQueue)
                mainNavigation.pop()
            }

            override fun playerView() {
                eventAnalytics.trackEvent(CommonEvents.Action.NavOpenPlayer)
                mainNavigation.push(MainConfig.Player)
            }

            override fun showEqualizer() {
                eventAnalytics.trackEvent(CommonEvents.Action.NavOpenEqualizer)
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
                    if (pages.value.active.configuration != PagesConfig.Catalog) {
                        eventAnalytics.trackEvent(MusicEvents.Action.NavOpenCatalog)
                    }
                    pagesNavigation.bringToFront(PagesConfig.Catalog)
                }
            }
        }

        override fun dismissDefaultPlayerPrompt() {
            playerSettings.markPromptShown()
            showDefaultPlayerPromptState.value = false
        }

        override fun confirmDefaultPlayerPrompt() {
            eventAnalytics.trackEvent(MusicEvents.Action.DefaultPlayerPromptClicked)
            playerSettings.markPromptShown()
            showDefaultPlayerPromptState.value = false
        }

        init {
            if (showDefaultPlayerPromptState.value) {
                eventAnalytics.trackEvent(MusicEvents.Action.DefaultPlayerPromptShown)
            }
            launch {
                pages.trackScreens<PagesConfig, MusicEvents.Screen>(
                    trackScreen = screenAnalytics::trackScreen,
                    name = "PagesConfig",
                ) {
                    when (it) {
                        PagesConfig.Queue -> MusicEvents.Screen.Queue
                        PagesConfig.Catalog -> MusicEvents.Screen.CatalogTab
                    }
                }
            }
            launch {
                mainComponent.trackScreens<MainConfig, CommonEvents.Screen>(
                    trackScreen = screenAnalytics::trackScreen,
                    name = "MainConfig",
                ) {
                    when (it) {
                        MainConfig.Main -> CommonEvents.Screen.RootOverlay
                        MainConfig.Player -> CommonEvents.Screen.Player
                        MainConfig.Equalizer -> CommonEvents.Screen.Equalizer
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
