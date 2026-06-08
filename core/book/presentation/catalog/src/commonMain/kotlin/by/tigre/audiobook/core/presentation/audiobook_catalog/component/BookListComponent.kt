package by.tigre.audiobook.core.presentation.audiobook_catalog.component

import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogDependency
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.OnBookSelectedListener
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.tools.analytics.book.AudiobookEvents
import by.tigre.music.player.tools.analytics.book.BookEventAnalytics
import by.tigre.music.player.presentation.base.ScreenContentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

interface BookListComponent {

    val screenState: StateFlow<ScreenContentState<BookListUiState>>

    fun onBookClicked(book: Book)
    fun onManageFolders()
    fun retry()
    fun toggleGroup(path: String)
    fun focusCurrentBook()

    data class BookListUiState(
        val continueListeningBook: Book?,
        val rootBooks: List<Book>,
        val grouped: List<Pair<String, List<Book>>>,
        val expanded: Set<String>,
        val currentBookId: Book.Id?,
        val scrollToBookNonce: Long,
    )

    class Impl(
        context: BaseComponentContext,
        dependency: AudiobookCatalogDependency,
        private val navigator: AudiobookCatalogNavigator,
        private val onBookSelectedListener: OnBookSelectedListener
    ) : BookListComponent, BaseComponentContext by context {

        private val catalogSource: AudiobookCatalogSource = dependency.audiobookCatalogSource
        private val playbackController: AudiobookPlaybackController = dependency.audiobookPlaybackController
        private val eventAnalytics: BookEventAnalytics = dependency.eventAnalytics

        private val expandedState = MutableStateFlow(emptySet<String>())
        private val scrollToBookNonce = MutableStateFlow(0L)

        override val screenState: StateFlow<ScreenContentState<BookListUiState>> = combine(
            catalogSource.books,
            playbackController.currentBook,
            expandedState,
            scrollToBookNonce,
        ) { books, currentBook, expanded, scrollNonce ->
            val currentBookId = currentBook?.id
            val continueListeningBook = currentBook?.takeUnless { it.isCompleted }
            val otherBooks = if (currentBookId != null) {
                books.filter { it.id != currentBookId }
            } else {
                books
            }
            val rootBooks = otherBooks.filter { it.subPath.isEmpty() }
            val grouped = otherBooks
                .filter { it.subPath.isNotEmpty() }
                .groupBy { it.subPath }
                .entries
                .sortedBy { it.key }
                .map { it.key to it.value }
            val expandedWithCurrent = if (currentBook?.subPath?.isNotEmpty() == true) {
                expanded + currentBook.subPath
            } else {
                expanded
            }
            ScreenContentState.Content(
                BookListUiState(
                    continueListeningBook = continueListeningBook,
                    rootBooks = rootBooks,
                    grouped = grouped,
                    expanded = expandedWithCurrent,
                    currentBookId = currentBookId,
                    scrollToBookNonce = scrollNonce,
                )
            )
        }
            .stateIn(this, SharingStarted.WhileSubscribed(), ScreenContentState.Loading)

        override fun onBookClicked(book: Book) {
            eventAnalytics.trackEvent(AudiobookEvents.Action.CatalogSelectBook)
            playbackController.loadBook(book)
            onBookSelectedListener.onBookSelected()
        }

        override fun onManageFolders() {
            eventAnalytics.trackEvent(AudiobookEvents.Action.CatalogOpenFolderSettings)
            navigator.showFolderSelection()
        }

        override fun retry() {

        }

        override fun toggleGroup(path: String) {
            expandedState.update { current ->
                if (current.contains(path)) current - path else current + path
            }
        }

        override fun focusCurrentBook() {
            scrollToBookNonce.update { it + 1L }
        }
    }
}
