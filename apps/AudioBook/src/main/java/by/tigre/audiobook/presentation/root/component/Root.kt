package by.tigre.audiobook.presentation.root.component

import by.tigre.audiobook.core.presentation.audiobook_catalog.component.RootAudiobookCatalogComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogComponentProvider
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.OnBookSelectedListener
import by.tigre.audiobook.platform.AudiobookGuideSettings
import by.tigre.media.platform.player.component.EqualizerComponent
import by.tigre.media.platform.player.component.PlayerComponent
import by.tigre.media.platform.player.component.SmallPlayerComponent
import by.tigre.media.platform.player.di.PlayerComponentProvider
import by.tigre.media.platform.player.navigation.PlayerNavigator
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.presentation.appChildContext
import by.tigre.media.platform.presentation.appChildStack
import by.tigre.media.platform.presentation.trackScreens
import by.tigre.media.platform.tools.analytics.book.AudiobookEvents
import by.tigre.media.platform.tools.analytics.book.BookEventAnalytics
import by.tigre.media.platform.tools.analytics.book.BookScreenAnalytics
import by.tigre.media.platform.tools.analytics.common.AnalyticsScreen
import by.tigre.media.platform.tools.analytics.common.CommonEvents
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.pushToFront
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
    val audiobookCatalogComponent: RootAudiobookCatalogComponent

    val onStartServiceEvent: Flow<Unit>

    val showGettingStartedGuide: StateFlow<Boolean>

    val mainComponent: Value<ChildStack<*, MainComponentChild>>

    fun onShowCatalog()

    fun onOpenFolderSettings()

    fun onOpenNightTimerSettings()

    fun onCloseNightTimerSettings()

    fun onOpenPlaybackSpeedSettings()

    fun onClosePlaybackSpeedSettings()

    fun dismissGettingStartedGuide()

    fun openFolderFromGettingStartedGuide()

    sealed interface MainComponentChild {
        data object Main : MainComponentChild
        class Player(val component: PlayerComponent) : MainComponentChild
        class Equalizer(val component: EqualizerComponent) : MainComponentChild

        data object NightTimerSettings : MainComponentChild

        data object PlaybackSpeedSettings : MainComponentChild
    }

    class Impl(
        context: BaseComponentContext,
        playerComponentProvider: PlayerComponentProvider,
        audiobookCatalogComponentProvider: AudiobookCatalogComponentProvider,
        screenAnalytics: BookScreenAnalytics,
        private val eventAnalytics: BookEventAnalytics,
        private val audiobookGuideSettings: AudiobookGuideSettings,
    ) : Root, BaseComponentContext by context {

        private val showGettingStartedGuideState =
            MutableStateFlow(audiobookGuideSettings.shouldShowGuide())
        override val showGettingStartedGuide: StateFlow<Boolean> =
            showGettingStartedGuideState.asStateFlow()

        private val mainNavigation = StackNavigation<MainConfig>()

        private val playerNavigator = object : PlayerNavigator {
            override fun showQueue() {
                mainNavigation.pop()
            }

            override fun playerView() {
                eventAnalytics.trackEvent(CommonEvents.Action.NavOpenPlayer)
                mainNavigation.pushToFront(MainConfig.Player)
            }

            override fun showEqualizer() {
                eventAnalytics.trackEvent(CommonEvents.Action.NavOpenEqualizer)
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

                    MainConfig.PlaybackSpeed -> MainComponentChild.PlaybackSpeedSettings
                }
            }

        override fun onShowCatalog() {
            eventAnalytics.trackEvent(AudiobookEvents.Action.NavOpenCatalog)
            mainNavigation.pushToFront(MainConfig.Main)
            audiobookCatalogComponent.focusCurrentBookInLibrary()
        }

        override fun onOpenFolderSettings() {
            eventAnalytics.trackEvent(AudiobookEvents.Action.CatalogOpenFolderSettings)
            mainNavigation.pushToFront(MainConfig.Main)
            audiobookCatalogComponent.openFolderSelection()
        }

        override fun onOpenNightTimerSettings() {
            eventAnalytics.trackEvent(AudiobookEvents.Action.NavOpenNightTimer)
            mainNavigation.push(MainConfig.NightTimer)
        }

        override fun onCloseNightTimerSettings() {
            mainNavigation.pop()
        }

        override fun onOpenPlaybackSpeedSettings() {
            mainNavigation.push(MainConfig.PlaybackSpeed)
        }

        override fun onClosePlaybackSpeedSettings() {
            mainNavigation.pop()
        }

        override fun dismissGettingStartedGuide() {
            audiobookGuideSettings.markGuideShown()
            showGettingStartedGuideState.value = false
        }

        override fun openFolderFromGettingStartedGuide() {
            dismissGettingStartedGuide()
            onOpenFolderSettings()
        }

        init {
            launch {
                mainComponent.trackScreens<MainConfig, AnalyticsScreen>(
                    trackScreen = { screen ->
                        when (screen) {
                            is CommonEvents.Screen -> screenAnalytics.trackScreen(screen)
                            is AudiobookEvents.Screen -> screenAnalytics.trackScreen(screen)
                        }
                    },
                    name = "MainConfig",
                ) {
                    when (it) {
                        MainConfig.Main -> AudiobookEvents.Screen.Catalog
                        MainConfig.NightTimer -> AudiobookEvents.Screen.NightTimerSettings
                        MainConfig.PlaybackSpeed -> AudiobookEvents.Screen.PlaybackSpeedSettings
                        MainConfig.Player -> CommonEvents.Screen.Player
                        MainConfig.Equalizer -> CommonEvents.Screen.Equalizer
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

            @Serializable
            data object PlaybackSpeed : MainConfig
        }
    }
}
