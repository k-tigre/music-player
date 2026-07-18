package by.tigre.audiobook.core.presentation.audiobook_catalog.component

import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogDependency
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.OnBookSelectedListener
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.tools.analytics.book.AudiobookEvents
import by.tigre.media.platform.tools.analytics.book.BookEventAnalytics
import by.tigre.media.platform.presentation.ScreenContentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface BookListComponent {

    val screenState: StateFlow<ScreenContentState<BookListUiState>>

    fun onBookClicked(book: Book)
    fun onOpenSettings()
    fun retry()
    fun toggleGroup(path: String)
    fun toggleContinueListening()
    fun onScreenShown()
    fun focusCurrentBook()
    fun dismissContinueListening(book: Book)

    data class BookListUiState(
        val continueListeningBooks: List<Book>,
        val continueListeningExpanded: Boolean,
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
        private val continueListeningExpandedState = MutableStateFlow(true)
        private val scrollToBookNonce = MutableStateFlow(0L)

        override val screenState: StateFlow<ScreenContentState<BookListUiState>> = combine(
            combine(
                catalogSource.books,
                catalogSource.continueListeningBooks,
                playbackController.currentBook,
            ) { books, continueListeningBooks, currentBook ->
                Triple(books, continueListeningBooks, currentBook)
            },
            expandedState,
            continueListeningExpandedState,
            scrollToBookNonce,
        ) { catalog, expanded, continueExpanded, scrollNonce ->
            val (books, continueListeningBooks, currentBook) = catalog
            val currentBookId = currentBook?.id
            val rootBooks = books.filter { it.subPath.isEmpty() }
            val grouped = books
                .filter { it.subPath.isNotEmpty() }
                .groupBy { it.subPath }
                .entries
                .sortedBy { it.key }
                .map { it.key to it.value }
            ScreenContentState.Content(
                BookListUiState(
                    continueListeningBooks = continueListeningBooks,
                    continueListeningExpanded = continueExpanded,
                    rootBooks = rootBooks,
                    grouped = grouped,
                    expanded = expanded,
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

        override fun onOpenSettings() {
            eventAnalytics.trackEvent(AudiobookEvents.Action.CatalogOpenSettings)
            navigator.showSettings()
        }

        override fun retry() {

        }

        override fun toggleGroup(path: String) {
            expandedState.update { current ->
                if (current.contains(path)) current - path else current + path
            }
        }

        override fun toggleContinueListening() {
            continueListeningExpandedState.update { expanded -> !expanded }
        }

        override fun onScreenShown() {
            continueListeningExpandedState.value = true
            val subPath = playbackController.currentBook.value?.subPath.orEmpty()
            if (subPath.isNotEmpty()) {
                expandedState.update { it + subPath }
            }
        }

        override fun focusCurrentBook() {
            onScreenShown()
            scrollToBookNonce.update { it + 1L }
        }

        override fun dismissContinueListening(book: Book) {
            launch {
                catalogSource.setHiddenFromContinueListening(book.id, hidden = true)
            }
        }
    }
}
