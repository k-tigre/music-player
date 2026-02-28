package by.tigre.audiobook.core.presentation.audiobook_catalog.component

import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogComponentProvider
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.appChildStack
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable

interface RootAudiobookCatalogComponent {

    val childStack: Value<ChildStack<*, AudiobookCatalogChild>>

    sealed interface AudiobookCatalogChild {
        class FolderSelection(val component: FolderSelectionComponent) : AudiobookCatalogChild
        class BookList(val component: BookListComponent) : AudiobookCatalogChild
    }

    class Impl(
        context: BaseComponentContext,
        private val componentProvider: AudiobookCatalogComponentProvider
    ) : RootAudiobookCatalogComponent, BaseComponentContext by context {

        private val navigation = StackNavigation<Config>()

        private val navigator = object : AudiobookCatalogNavigator {
            override fun showFolderSelection() {
                navigation.bringToFront(Config.FolderSelection)
            }

            override fun showBookList() {
                navigation.bringToFront(Config.BookList)
            }

            override fun showPreviousScreen() {
                navigation.pop()
            }
        }

        private val stack = appChildStack(
            source = navigation,
            initialStack = { listOf(Config.BookList) },
            childFactory = ::child,
            handleBackButton = true
        )

        private fun child(config: Config, context: BaseComponentContext): AudiobookCatalogChild =
            when (config) {
                is Config.FolderSelection -> AudiobookCatalogChild.FolderSelection(
                    componentProvider.createFolderSelectionComponent(context, navigator)
                )

                is Config.BookList -> AudiobookCatalogChild.BookList(
                    componentProvider.createBookListComponent(context, navigator)
                )
            }

        override val childStack: Value<ChildStack<*, AudiobookCatalogChild>> = stack

        @Serializable
        private sealed interface Config {
            @Serializable
            data object FolderSelection : Config

            @Serializable
            data object BookList : Config
        }
    }
}
