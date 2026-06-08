package by.tigre.audiobook.presentation.root.component

import by.tigre.audiobook.core.presentation.audiobook_catalog.component.RootAudiobookCatalogComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogComponentProvider
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.OnBookSelectedListener
import by.tigre.music.player.core.presentation.catalog.component.EqualizerComponent
import by.tigre.music.player.core.presentation.catalog.component.PlayerComponent
import by.tigre.music.player.core.presentation.catalog.component.SmallPlayerComponent
import by.tigre.music.player.core.presentation.catalog.di.PlayerComponentProvider
import by.tigre.music.player.core.presentation.catalog.navigation.PlayerNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.appChildContext
import by.tigre.music.player.presentation.base.appChildStack
import by.tigre.music.player.presentation.base.trackScreens
import by.tigre.music.player.tools.analytics.Event
import by.tigre.music.player.tools.analytics.EventAnalytics
import by.tigre.music.player.tools.analytics.ScreenAnalytics
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface Root {

    val playerComponent: SmallPlayerComponent
    val audiobookCatalogComponent: RootAudiobookCatalogComponent

    val onStartServiceEvent: Flow<Unit>

    val mainComponent: Value<ChildStack<*, MainComponentChild>>

    fun onShowCatalog()

    fun onOpenFolderSettings()

    fun onOpenNightTimerSettings()

    fun onCloseNightTimerSettings()

    sealed interface MainComponentChild {
        data object Main : MainComponentChild
        class Player(val component: PlayerComponent) : MainComponentChild
        class Equalizer(val component: EqualizerComponent) : MainComponentChild

        data object NightTimerSettings : MainComponentChild
    }

    class Impl(
        context: BaseComponentContext,
        playerComponentProvider: PlayerComponentProvider,
        audiobookCatalogComponentProvider: AudiobookCatalogComponentProvider,
        screenAnalytics: ScreenAnalytics,
        private val eventAnalytics: EventAnalytics,
    ) : Root, BaseComponentContext by context {

        private val mainNavigation = StackNavigation<MainConfig>()

        private val playerNavigator = object : PlayerNavigator {
            override fun showQueue() {
                mainNavigation.pop()
            }

            override fun playerView() {
                eventAnalytics.trackEvent(Event.Action.UI.Button.OpenPlayer)
                mainNavigation.pushToFront(MainConfig.Player)
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

        override val audiobookCatalogComponent: RootAudiobookCatalogComponent by lazy {
            audiobookCatalogComponentProvider.createRootAudiobookCatalogComponent(
                context = appChildContext("audiobook_catalog"),
                onBookSelectedListener = object : OnBookSelectedListener {
                    override fun onBookSelected() {
                        playerNavigator.playerView()
                    }
                }
            )
        }

        override val onStartServiceEvent = MutableSharedFlow<Unit>()

        override val mainComponent: Value<ChildStack<*, MainComponentChild>> =
            appChildStack(
                source = mainNavigation,
                initialStack = { listOf(MainConfig.Player) },
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

                    MainConfig.NightTimer -> MainComponentChild.NightTimerSettings
                }
            }

        override fun onShowCatalog() {
            eventAnalytics.trackEvent(Event.Action.UI.Button.OpenCatalog)
            mainNavigation.pushToFront(MainConfig.Main)
            audiobookCatalogComponent.focusCurrentBookInLibrary()
        }

        override fun onOpenFolderSettings() {
            eventAnalytics.trackEvent(Event.Action.UI.Button.OpenFolderSettings)
            mainNavigation.pushToFront(MainConfig.Main)
            audiobookCatalogComponent.openFolderSelection()
        }

        override fun onOpenNightTimerSettings() {
            eventAnalytics.trackEvent(Event.Action.UI.Button.OpenNightTimer)
            mainNavigation.push(MainConfig.NightTimer)
        }

        override fun onCloseNightTimerSettings() {
            mainNavigation.pop()
        }

        init {
            launch {
                mainComponent.trackScreens<MainConfig>(screenAnalytics, "MainConfig") {
                    when (it) {
                        MainConfig.Main -> Event.Screen.AudiobookCatalog
                        MainConfig.Player -> Event.Screen.Player
                        MainConfig.Equalizer -> Event.Screen.Equalizer
                        MainConfig.NightTimer -> Event.Screen.NightTimerSettings
                    }
                }
            }
        }

        @Serializable
        private sealed interface MainConfig {
            @Serializable
            data object Main : MainConfig

            @Serializable
            data object Player : MainConfig

            @Serializable
            data object Equalizer : MainConfig

            @Serializable
            data object NightTimer : MainConfig
        }
    }
}
