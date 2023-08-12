package by.tigre.music.player.presentation.root.component

import by.tigre.music.player.core.presentation.catalog.component.RootCatalogComponent
import by.tigre.music.player.core.presentation.catalog.component.SmallPlayerComponent
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.PlayerComponentProvider
import by.tigre.music.player.core.presentation.playlist.current.component.CurrentQueueComponent
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueComponentProvider
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.appChildContext
import by.tigre.music.player.presentation.base.appChildStack
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.parcelize.Parcelize

interface Root {

    val playerComponent: SmallPlayerComponent

    val onStartServiceEvent: Flow<Unit>

    val pages: Value<ChildStack<*, PageComponentChild>>

    fun selectPage(index: Int)

    sealed interface PageComponentChild {
        class Queue(val component: CurrentQueueComponent) : PageComponentChild
        class Catalog(val component: RootCatalogComponent) : PageComponentChild
    }

    class Impl(
        context: BaseComponentContext,
        catalogComponentProvider: CatalogComponentProvider,
        playerComponentProvider: PlayerComponentProvider,
        currentQueueComponent: CurrentQueueComponentProvider,
    ) : Root, BaseComponentContext by context {

        private val navigation = StackNavigation<Config>()

        override val playerComponent: SmallPlayerComponent by lazy {
            playerComponentProvider.createSmallPlayerComponent(
                context = appChildContext("player"),
                navigator = {
                    /*open player view*/
                }
            )
        }

        override val onStartServiceEvent = MutableSharedFlow<Unit>()

        override val pages: Value<ChildStack<*, PageComponentChild>> =
            appChildStack(
                source = navigation,
                initialStack = { listOf(Config.Queue) },
            ) { config, componentContext ->
                when (config) {
                    Config.Catalog -> PageComponentChild.Catalog(
                        catalogComponentProvider.createRootCatalogComponent(componentContext)
                    )

                    Config.Queue -> PageComponentChild.Queue(
                        currentQueueComponent.createCurrentQueueComponent(
                            componentContext,
                            navigator = { selectPage(1) }
                        )
                    )
                }
            }

        override fun selectPage(index: Int) {
            when (index) {
                0 -> navigation.bringToFront(Config.Queue)
                1 -> navigation.bringToFront(Config.Catalog)
            }
        }

        private sealed interface Config : Parcelable {
            @Parcelize
            data object Queue : Config

            @Parcelize
            data object Catalog : Config
        }
    }
}
