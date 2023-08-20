package by.tigre.music.player.presentation.root.component

import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.presentation.backgound_player.di.PlayerBackgroundDependency
import by.tigre.music.player.core.presentation.catalog.component.RootCatalogComponent
import by.tigre.music.player.core.presentation.catalog.component.SmallPlayerComponent
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.PlayerComponentProvider
import by.tigre.music.player.core.presentation.playlist.current.component.CurrentQueueComponent
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueComponentProvider
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.appChildContext
import by.tigre.music.player.presentation.base.appChildPages
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.select
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@OptIn(ExperimentalDecomposeApi::class)
interface Root {

    val playerComponent: SmallPlayerComponent

    val onStartServiceEvent: Flow<Unit>

    val pages: Value<ChildPages<*, PageComponentChild>>

    fun selectPage(index: Int)

    sealed interface PageComponentChild {
        class Queue(val component: CurrentQueueComponent) : PageComponentChild
        class Catalog(val component: RootCatalogComponent) : PageComponentChild
    }

    @OptIn(FlowPreview::class)
    class Impl(
        context: BaseComponentContext,
        catalogComponentProvider: CatalogComponentProvider,
        playerComponentProvider: PlayerComponentProvider,
        currentQueueComponent: CurrentQueueComponentProvider,
        dependency: PlayerBackgroundDependency
    ) : Root, BaseComponentContext by context {

        private val playbackController = dependency.playbackController

        override val playerComponent: SmallPlayerComponent by lazy {
            playerComponentProvider.createSmallPlayerComponent(
                appChildContext("player"),
                navigator = {
                    selectPage(0)
                })
        }

        override val onStartServiceEvent = MutableSharedFlow<Unit>()

        private val navigation = PagesNavigation<Config>()

        override val pages: Value<ChildPages<*, PageComponentChild>> =
            appChildPages(
                source = navigation,
                initialPages = {
                    Pages(
                        items = listOf(Config.Queue, Config.Catalog),
                        selectedIndex = 0,
                    )
                },
            ) { config, componentContext ->
                when (config) {
                    Config.Catalog -> PageComponentChild.Catalog(
                        catalogComponentProvider.createRootCatalogComponent(componentContext)
                    )

                    Config.Queue -> PageComponentChild.Queue(
                        currentQueueComponent.createCurrentQueueComponent(componentContext,
                            navigator = { selectPage(1) }
                        )
                    )
                }
            }

        init {
            launch {
                playbackController.player.state
                    .map { it == PlaybackPlayer.State.Playing }
                    .debounce(50)
                    .distinctUntilChanged()
                    .filter { it }
                    .collect { onStartServiceEvent.emit(Unit) }
            }
        }

        override fun selectPage(index: Int) {
            navigation.select(index = index)
        }


        private sealed interface Config : Parcelable {
            @Parcelize
            data object Queue : Config

            @Parcelize
            data object Catalog : Config
        }
    }
}
