package by.tigre.audiobook.core.presentation.audiobook_catalog.component

import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook.spaces.LibrarySpaceRepository
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.LibrarySpace
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogDependency
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.OnBookSelectedListener
import by.tigre.media.platform.entitlements.Feature
import by.tigre.media.platform.entitlements.FeatureAccess
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.tools.analytics.book.AudiobookEvents
import by.tigre.media.platform.tools.analytics.book.BookEventAnalytics
import by.tigre.media.platform.presentation.ScreenContentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface BookListComponent {

    val screenState: StateFlow<ScreenContentState<BookListUiState>>
    val spaceSheetVisible: StateFlow<Boolean>
    val addBooksSheetVisible: StateFlow<Boolean>
    val addBooksPicker: StateFlow<AddBooksPickerState>
    val editMembership: StateFlow<Boolean>

    fun onBookClicked(book: Book)
    fun onOpenSettings()
    fun retry()
    fun toggleGroup(path: String)
    fun toggleContinueListening()
    fun onScreenShown()
    fun focusCurrentBook()
    fun dismissContinueListening(book: Book)
    fun requestMoreContinueListening()
    fun onSpaceChipClicked()
    fun dismissSpaceSheet()
    fun onSpaceSelected(spaceId: LibrarySpace.Id)
    fun onCreateSpaceClicked()
    fun onConfirmCreateSpace(name: String)
    fun onAddBooksClicked()
    fun dismissAddBooksSheet()
    fun togglePickerBook(bookId: Book.Id)
    fun confirmAddSelectedBooks()
    fun addPickerFolder(subPath: String)
    fun onEditMembershipClicked()
    fun onDoneEditMembership()
    fun onRemoveBookFromSpace(bookId: Book.Id)
    fun onRemoveFolderFromSpace(subPath: String)

    data class BookListUiState(
        val continueListeningBooks: List<Book>,
        val continueListeningTotalCount: Int,
        val continueListeningHasMore: Boolean,
        val continueListeningExpanded: Boolean,
        val rootBooks: List<Book>,
        val grouped: List<Pair<String, List<Book>>>,
        val expanded: Set<String>,
        val currentBookId: Book.Id?,
        val scrollToBookNonce: Long,
        val spacesVisible: Boolean,
        val activeSpace: LibrarySpace?,
        val spaces: List<LibrarySpace>,
        val canManageSpaces: Boolean,
        val emptySpaceNeedsBooks: Boolean,
        val editMembership: Boolean,
    )

    data class AddBooksPickerState(
        val candidates: List<Book> = emptyList(),
        val selectedIds: Set<Book.Id> = emptySet(),
        val folders: List<Pair<String, Int>> = emptyList(),
    )

    class Impl(
        context: BaseComponentContext,
        dependency: AudiobookCatalogDependency,
        private val navigator: AudiobookCatalogNavigator,
        private val onBookSelectedListener: OnBookSelectedListener
    ) : BookListComponent, BaseComponentContext by context {

        private val catalogSource: AudiobookCatalogSource = dependency.audiobookCatalogSource
        private val playbackController: AudiobookPlaybackController = dependency.audiobookPlaybackController
        private val spaceRepository: LibrarySpaceRepository = dependency.librarySpaceRepository
        private val eventAnalytics: BookEventAnalytics = dependency.eventAnalytics
        private val entitlementsRepository = dependency.entitlementsRepository
        private val requestPaywall = dependency::requestPaywall

        private val expandedState = MutableStateFlow(emptySet<String>())
        private val continueListeningExpandedState = MutableStateFlow(true)
        private val scrollToBookNonce = MutableStateFlow(0L)
        private val editMembershipState = MutableStateFlow(false)
        override val spaceSheetVisible = MutableStateFlow(false)
        override val addBooksSheetVisible = MutableStateFlow(false)
        override val addBooksPicker = MutableStateFlow(AddBooksPickerState())
        override val editMembership: StateFlow<Boolean> = editMembershipState.asStateFlow()

        override val screenState: StateFlow<ScreenContentState<BookListUiState>> = combine(
            combine(
                catalogSource.books,
                catalogSource.continueListeningBooks,
                playbackController.currentBook,
            ) { books, continueListeningBooks, currentBook ->
                Triple(books, continueListeningBooks, currentBook)
            },
            combine(
                expandedState,
                continueListeningExpandedState,
                scrollToBookNonce,
                spaceRepository.activeSpace,
                spaceRepository.spaces,
            ) { expanded, continueExpanded, scrollNonce, activeSpace, spaces ->
                SpacesUi(expanded, continueExpanded, scrollNonce, activeSpace, spaces)
            },
            combine(
                dependency.entitlementsRepository.tier,
                editMembershipState,
            ) { _, editMembership -> editMembership },
        ) { catalog, spacesUi, editMembership ->
            val (books, continueListeningBooks, currentBook) = catalog
            val currentBookId = currentBook?.id
            val rootBooks = books.filter { it.subPath.isEmpty() }
            val grouped = books
                .filter { it.subPath.isNotEmpty() }
                .groupBy { it.subPath }
                .entries
                .sortedBy { it.key }
                .map { it.key to it.value }
            val limit = entitlementsRepository.continueListeningLimit()
            val visibleContinue = continueListeningBooks.take(limit)
            val spacesAccess = entitlementsRepository.access(Feature.BookSpaces)
            val spacesVisible = spacesAccess != FeatureAccess.Unavailable
            val canManage = spacesAccess == FeatureAccess.Allowed
            ScreenContentState.Content(
                BookListUiState(
                    continueListeningBooks = visibleContinue,
                    continueListeningTotalCount = continueListeningBooks.size,
                    continueListeningHasMore = continueListeningBooks.size > visibleContinue.size,
                    continueListeningExpanded = spacesUi.continueExpanded,
                    rootBooks = rootBooks,
                    grouped = grouped,
                    expanded = spacesUi.expanded,
                    currentBookId = currentBookId,
                    scrollToBookNonce = spacesUi.scrollNonce,
                    spacesVisible = spacesVisible,
                    activeSpace = spacesUi.activeSpace,
                    spaces = spacesUi.spaces,
                    canManageSpaces = canManage,
                    emptySpaceNeedsBooks = books.isEmpty() &&
                        spacesUi.activeSpace != null &&
                        spacesUi.activeSpace?.isDefault != true,
                    editMembership = editMembership && spacesVisible,
                )
            )
        }
            .stateIn(this, SharingStarted.WhileSubscribed(), ScreenContentState.Loading)

        init {
            launch {
                var previousSpaceId: LibrarySpace.Id? = null
                spaceRepository.activeSpaceId.collect { spaceId ->
                    if (previousSpaceId != null && previousSpaceId != spaceId) {
                        editMembershipState.value = false
                    }
                    previousSpaceId = spaceId
                }
            }
        }

        override fun onBookClicked(book: Book) {
            if (editMembershipState.value) return
            eventAnalytics.trackEvent(AudiobookEvents.Action.CatalogSelectBook)
            playbackController.loadBook(book)
            onBookSelectedListener.onBookSelected()
        }

        override fun onOpenSettings() {
            editMembershipState.value = false
            eventAnalytics.trackEvent(AudiobookEvents.Action.CatalogOpenSettings)
            navigator.showSettings()
        }

        override fun retry() = Unit

        override fun toggleGroup(path: String) {
            if (editMembershipState.value) {
                expandedState.update { it + path }
                return
            }
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

        override fun requestMoreContinueListening() {
            if (entitlementsRepository.access(Feature.ContinueListeningExpanded) ==
                FeatureAccess.RequiresPurchase
            ) {
                requestPaywall(Feature.ContinueListeningExpanded, "continue_listening")
            }
        }

        override fun onSpaceChipClicked() {
            if (editMembershipState.value) return
            spaceSheetVisible.value = true
        }

        override fun dismissSpaceSheet() {
            spaceSheetVisible.value = false
        }

        override fun onSpaceSelected(spaceId: LibrarySpace.Id) {
            applySpaceSwitch(spaceId)
        }

        override fun onCreateSpaceClicked() {
            when (entitlementsRepository.access(Feature.BookSpaces)) {
                FeatureAccess.Allowed -> Unit
                FeatureAccess.RequiresPurchase -> {
                    spaceSheetVisible.value = false
                    requestPaywall(Feature.BookSpaces, "library_spaces")
                }
                FeatureAccess.Unavailable -> spaceSheetVisible.value = false
            }
        }

        override fun onConfirmCreateSpace(name: String) {
            launch {
                val id = spaceRepository.createSpace(name = name.trim().ifBlank { "Kids" })
                if (id != null) {
                    applySpaceSwitch(id)
                } else if (entitlementsRepository.access(Feature.BookSpaces) ==
                    FeatureAccess.RequiresPurchase
                ) {
                    requestPaywall(Feature.BookSpaces, "library_spaces_limit")
                }
            }
        }

        private fun applySpaceSwitch(spaceId: LibrarySpace.Id) {
            val previous = spaceRepository.activeSpaceId.value
            if (previous == spaceId) {
                editMembershipState.value = false
                spaceSheetVisible.value = false
                return
            }
            spaceRepository.setActiveSpace(spaceId)
            editMembershipState.value = false
            spaceSheetVisible.value = false
            if (spaceRepository.activeSpaceId.value == previous) return
            launch {
                playbackController.adoptActiveSpaceAfterSwitch()
            }
        }

        override fun onAddBooksClicked() {
            launch { openAddBooksPicker() }
        }

        override fun dismissAddBooksSheet() {
            addBooksSheetVisible.value = false
            addBooksPicker.value = AddBooksPickerState()
        }

        override fun togglePickerBook(bookId: Book.Id) {
            addBooksPicker.update { state ->
                val next = if (bookId in state.selectedIds) state.selectedIds - bookId else state.selectedIds + bookId
                state.copy(selectedIds = next)
            }
        }

        override fun confirmAddSelectedBooks() {
            launch {
                val spaceId = spaceRepository.activeSpaceId.value
                val selected = addBooksPicker.value.selectedIds.toList()
                if (selected.isNotEmpty()) {
                    spaceRepository.addBooks(spaceId, selected)
                }
                dismissAddBooksSheet()
            }
        }

        override fun addPickerFolder(subPath: String) {
            launch {
                spaceRepository.addBooksBySubPath(spaceRepository.activeSpaceId.value, subPath)
                openAddBooksPicker()
            }
        }

        override fun onEditMembershipClicked() {
            editMembershipState.value = true
            expandedState.update { expanded ->
                val content = (screenState.value as? ScreenContentState.Content)?.value
                expanded + (content?.grouped?.map { it.first }.orEmpty())
            }
        }

        override fun onDoneEditMembership() {
            editMembershipState.value = false
        }

        override fun onRemoveBookFromSpace(bookId: Book.Id) {
            launch {
                spaceRepository.removeBook(spaceRepository.activeSpaceId.value, bookId)
            }
        }

        override fun onRemoveFolderFromSpace(subPath: String) {
            launch {
                spaceRepository.removeBooksBySubPath(spaceRepository.activeSpaceId.value, subPath)
            }
        }

        private suspend fun openAddBooksPicker() {
            val inSpace = catalogSource.getBooks().map { it.id }.toSet()
            val candidates = spaceRepository.getBooksGlobal().filter { it.id !in inSpace }
            val folders = candidates
                .groupBy { it.subPath }
                .map { (path, books) -> path to books.size }
                .sortedBy { it.first }
            addBooksPicker.value = AddBooksPickerState(
                candidates = candidates,
                selectedIds = emptySet(),
                folders = folders,
            )
            addBooksSheetVisible.value = true
        }

        private data class SpacesUi(
            val expanded: Set<String>,
            val continueExpanded: Boolean,
            val scrollNonce: Long,
            val activeSpace: LibrarySpace?,
            val spaces: List<LibrarySpace>,
        )
    }
}
