package by.tigre.audiobook.presentation.root.component

import by.tigre.audiobook.core.presentation.audiobook_catalog.component.RootAudiobookCatalogComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.component.PlayerComponent
import by.tigre.music.player.core.presentation.catalog.component.SmallPlayerComponent
import by.tigre.music.player.core.presentation.catalog.di.PlayerComponentProvider
import by.tigre.music.player.core.presentation.catalog.navigation.PlayerNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.appChildContext
import by.tigre.music.player.presentation.base.appChildStack
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.parcelize.Parcelize

interface Root {

    val playerComponent: SmallPlayerComponent
    val audiobookCatalogComponent: RootAudiobookCatalogComponent

    val onStartServiceEvent: Flow<Unit>

    val mainComponent: Value<ChildStack<*, MainComponentChild>>

    sealed interface MainComponentChild {
        data object Main : MainComponentChild
        class Player(val component: PlayerComponent) : MainComponentChild
    }

    class Impl(
        context: BaseComponentContext,
        playerComponentProvider: PlayerComponentProvider,
        audiobookCatalogComponentProvider: AudiobookCatalogComponentProvider,
    ) : Root, BaseComponentContext by context {

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

        override val audiobookCatalogComponent: RootAudiobookCatalogComponent by lazy {
            audiobookCatalogComponentProvider.createRootAudiobookCatalogComponent(
                appChildContext("audiobook_catalog")
            )
        }

        override val onStartServiceEvent = MutableSharedFlow<Unit>()

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

        private sealed interface MainConfig : Parcelable {
            @Parcelize
            data object Main : MainConfig

            @Parcelize
            data object Player : MainConfig
        }
    }
}
