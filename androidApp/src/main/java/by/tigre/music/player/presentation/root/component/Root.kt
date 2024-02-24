package by.tigre.music.player.presentation.root.component

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
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.parcelize.Parcelize

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
    }

    class Impl(
        context: BaseComponentContext,
        catalogComponentProvider: CatalogComponentProvider,
        playerComponentProvider: PlayerComponentProvider,
        currentQueueComponent: CurrentQueueComponentProvider,
    ) : Root, BaseComponentContext by context {

        private val pagesNavigation = StackNavigation<PagesConfig>()
        private val mainNavigation = StackNavigation<MainConfig>()

        private val playerNavigator = object : PlayerNavigator {
            override fun showQueue() {
                mainNavigation.pop()
            }

            override fun playerView() {
                mainNavigation.push(MainConfig.Player)
            }
        }

        override val playerComponent: SmallPlayerComponent by lazy {
            playerComponentProvider.createSmallPlayerComponent(
                context = appChildContext("player"),
                navigator = playerNavigator
            )
        }

        override val onStartServiceEvent = MutableSharedFlow<Unit>()

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
                }
            }

        override fun selectPage(index: Int) {
            when (index) {
                0 -> pagesNavigation.bringToFront(PagesConfig.Queue)
                1 -> pagesNavigation.bringToFront(PagesConfig.Catalog)
            }
        }

        private sealed interface PagesConfig : Parcelable {
            @Parcelize
            data object Queue : PagesConfig

            @Parcelize
            data object Catalog : PagesConfig
        }

        private sealed interface MainConfig : Parcelable {
            @Parcelize
            data object Main : MainConfig

            @Parcelize
            data object Player : MainConfig
        }

        @Parcelize
        private object Player : Parcelable
    }
}
