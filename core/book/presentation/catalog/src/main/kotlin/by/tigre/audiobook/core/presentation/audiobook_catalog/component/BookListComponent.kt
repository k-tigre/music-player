package by.tigre.audiobook.core.presentation.audiobook_catalog.component

import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogDependency
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.OnBookSelectedListener
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.ScreenContentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

interface BookListComponent {

    val screenState: StateFlow<ScreenContentState<BookListUiState>>

    fun onBookClicked(book: Book)
    fun onManageFolders()
    fun retry()
    fun toggleGroup(path: String)

    data class BookListUiState(
        val rootBooks: List<Book>,
        val grouped: List<Pair<String, List<Book>>>,
        val expanded: Set<String>
    )

    class Impl(
        context: BaseComponentContext,
        dependency: AudiobookCatalogDependency,
        private val navigator: AudiobookCatalogNavigator,
        private val onBookSelectedListener: OnBookSelectedListener
    ) : BookListComponent, BaseComponentContext by context {

        private val catalogSource: AudiobookCatalogSource = dependency.audiobookCatalogSource
        private val playbackController: AudiobookPlaybackController = dependency.audiobookPlaybackController

        private val expandedState = MutableStateFlow(emptySet<String>())

        override val screenState: StateFlow<ScreenContentState<BookListUiState>> = combine(
            catalogSource.books
                .map { books ->
                    val rootBooks = books.filter { it.subPath.isEmpty() }
                    val grouped = books
                        .filter { it.subPath.isNotEmpty() }
                        .groupBy { it.subPath }
                        .entries
                        .sortedBy { it.key }
                        .map { it.key to it.value }
                    rootBooks to grouped
                }, expandedState
        ) { (rootBooks, grouped), _ ->
            ScreenContentState.Content(
                BookListUiState(
                    rootBooks,
                    grouped,
                    expandedState.value
                )
            )
        }
            .stateIn(this, SharingStarted.WhileSubscribed(), ScreenContentState.Loading)

        override fun onBookClicked(book: Book) {
            playbackController.loadBook(book)
            onBookSelectedListener.onBookSelected()
        }

        override fun onManageFolders() {
            navigator.showFolderSelection()
        }

        override fun retry() {

        }

        override fun toggleGroup(path: String) {
            val current = expandedState.value
            expandedState.value = if (current.contains(path)) current - path else current + path
        }
    }
}
