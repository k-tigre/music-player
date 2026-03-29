package by.tigre.music.player.desktop.presentation.root.component

import by.tigre.music.player.core.presentation.catalog.component.EqualizerComponent
import by.tigre.music.player.core.presentation.catalog.component.PlayerComponent
import by.tigre.music.player.core.presentation.catalog.component.RootCatalogComponent
import by.tigre.music.player.core.presentation.catalog.component.SmallPlayerComponent
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.PlayerComponentProvider
import by.tigre.music.player.core.presentation.catalog.navigation.PlayerNavigator
import by.tigre.music.player.core.presentation.playlist.current.component.CurrentQueueComponent
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueComponentProvider
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.appChildContext
import by.tigre.music.player.presentation.base.appChildStack
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
    fun addCatalogFolder(folder: File)

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
        private val onAddFolder: suspend (File) -> Unit,
    ) : Root, BaseComponentContext by context {

        private val pagesNavigation = StackNavigation<PagesConfig>()
        private val mainNavigation = StackNavigation<MainConfig>()

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
        }

        override val playerComponent: SmallPlayerComponent by lazy {
            playerComponentProvider.createSmallPlayerComponent(
                context = appChildContext("player"),
                navigator = playerNavigator
            )
        }

        override val pages: Value<ChildStack<*, PageComponentChild>> =
            appChildStack(
                source = pagesNavigation,
                initialStack = { listOf(PagesConfig.Queue) },
                key = "pages",
                handleBackButton = false
            ) { config, componentContext ->
                when (config) {
                    PagesConfig.Catalog -> PageComponentChild.Catalog(
                        catalogComponentProvider.createRootCatalogComponent(componentContext)
                    )

                    PagesConfig.Queue -> PageComponentChild.Queue(
                        currentQueueComponent.createCurrentQueueComponent(
                            componentContext,
                            navigator = { selectPage(1) }
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
                1 -> pagesNavigation.bringToFront(PagesConfig.Catalog)
            }
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
