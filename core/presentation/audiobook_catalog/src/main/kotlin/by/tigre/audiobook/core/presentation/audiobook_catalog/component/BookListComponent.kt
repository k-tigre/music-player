package by.tigre.audiobook.core.presentation.audiobook_catalog.component

import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogDependency
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.presentation.base.ScreenContentStateDelegate
import kotlinx.coroutines.flow.StateFlow

interface BookListComponent {

    val screenState: StateFlow<ScreenContentState<List<Book>>>

    fun onBookClicked(book: Book)
    fun onManageFolders()
    fun retry()

    class Impl(
        context: BaseComponentContext,
        dependency: AudiobookCatalogDependency,
        private val navigator: AudiobookCatalogNavigator
    ) : BookListComponent, BaseComponentContext by context {

        private val catalogSource: AudiobookCatalogSource = dependency.audiobookCatalogSource

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = { catalogSource.books },
            mapDataToState = { books ->
                ScreenContentState.Content(books)
            }
        )

        override val screenState: StateFlow<ScreenContentState<List<Book>>> = stateDelegate.screenState

        override fun onBookClicked(book: Book) {
            // TODO: navigate to book detail / start playback
        }

        override fun onManageFolders() {
            navigator.showFolderSelection()
        }

        override fun retry() {
            stateDelegate.reload()
        }
    }
}
