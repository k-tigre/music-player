package by.tigre.audiobook.core.data.audiobook_playback.impl

import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook.CatalogScanDetail
import by.tigre.audiobook.core.data.audiobook.CatalogScanUi
import by.tigre.audiobook.core.data.audiobook.FolderSourceAccessHealth
import by.tigre.audiobook.core.data.audiobook.spaces.LibrarySpaceRepository
import by.tigre.audiobook.core.data.audiobook_playback.prefs.AudiobookPlaybackSpeedPreferences
import by.tigre.audiobook.core.data.storage.audiobook_playback.AudiobookPlaybackStorage
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.audiobook.core.entity.catalog.FolderSource
import by.tigre.audiobook.core.entity.catalog.LibrarySpace
import by.tigre.media.platform.playback.MediaItemWrapper
import by.tigre.media.platform.playback.PlaybackPlayer
import by.tigre.media.platform.preferences.Preferences
import by.tigre.media.platform.tools.coroutines.CoreScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * MediaSession Legacy often sends pause on STATE_ENDED (BT / system controls).
 * That must not cancel auto-advance or arm resume-rewind.
 */
class AudiobookPlaybackControllerImplTest {

    @Test
    fun pauseDuringEndedAutoAdvanceDoesNotStopNextChapter() = runBlocking {
        val book = testBook()
        val chapter1 = testChapter(id = 1, bookId = book.id, title = "001", durationMs = 60_000, sort = 0)
        val chapter2 = testChapter(id = 2, bookId = book.id, title = "002", durationMs = 60_000, sort = 1)
        val player = FakePlaybackPlayer(initialState = PlaybackPlayer.State.Playing, positionMs = 59_000)
        val controller = AudiobookPlaybackControllerImpl(
            player = player,
            catalog = FakeCatalog(book, listOf(chapter1, chapter2)),
            storage = FakeStorage(lastBookId = book.id, position = AudiobookPlaybackStorage.PlaybackPosition(chapter1.id, 59_000)),
            speedPreferences = AudiobookPlaybackSpeedPreferences(FakePreferences()),
            librarySpaceRepository = FakeSpaceRepository(),
            scope = TestCoreScope(),
        )

        waitForCondition { controller.currentChapter.value?.id == chapter1.id }
        controller.resume()
        waitForCondition { player.resumeCalls > 0 }
        player.resetCallCounts()

        // Fire MediaSession-style pause in the middle of setChapter (legacy onPause on ENDED).
        player.onSetMediaItem = { controller.pause() }
        player.setState(PlaybackPlayer.State.Ended)

        waitForCondition { controller.currentChapter.value?.id == chapter2.id }
        waitForCondition { player.resumeCalls > 0 }

        assertEquals(chapter2.id, controller.currentChapter.value?.id)
        assertEquals(0, player.pauseCalls, "external pause during Ended auto-advance must be ignored")
        assertTrue(player.resumeCalls > 0, "next chapter must keep playing")
    }

    @Test
    fun pauseWhilePlayingStillPauses() = runBlocking {
        val book = testBook()
        val chapter1 = testChapter(id = 1, bookId = book.id, title = "001", durationMs = 60_000, sort = 0)
        val player = FakePlaybackPlayer(initialState = PlaybackPlayer.State.Playing, positionMs = 10_000)
        val controller = AudiobookPlaybackControllerImpl(
            player = player,
            catalog = FakeCatalog(book, listOf(chapter1)),
            storage = FakeStorage(lastBookId = book.id, position = AudiobookPlaybackStorage.PlaybackPosition(chapter1.id, 10_000)),
            speedPreferences = AudiobookPlaybackSpeedPreferences(FakePreferences()),
            librarySpaceRepository = FakeSpaceRepository(),
            scope = TestCoreScope(),
        )

        waitForCondition { controller.currentChapter.value?.id == chapter1.id }
        controller.resume()
        waitForCondition { player.resumeCalls > 0 }
        player.resetCallCounts()

        controller.pause()
        waitForCondition { player.pauseCalls > 0 }

        assertEquals(1, player.pauseCalls)
        assertEquals(PlaybackPlayer.State.Paused, player.state.value)
    }

    @Test
    fun adoptActiveSpaceAfterSwitchClearsWhenNoLastBook() = runBlocking {
        val book = testBook()
        val chapter1 = testChapter(id = 1, bookId = book.id, title = "001", durationMs = 60_000, sort = 0)
        val storage = FakeStorage(
            lastBookId = book.id,
            position = AudiobookPlaybackStorage.PlaybackPosition(chapter1.id, 10_000),
        )
        val controller = AudiobookPlaybackControllerImpl(
            player = FakePlaybackPlayer(initialState = PlaybackPlayer.State.Paused, positionMs = 10_000),
            catalog = FakeCatalog(book, listOf(chapter1)),
            storage = storage,
            speedPreferences = AudiobookPlaybackSpeedPreferences(FakePreferences()),
            librarySpaceRepository = FakeSpaceRepository(),
            scope = TestCoreScope(),
        )

        waitForCondition { controller.currentBook.value?.id == book.id }
        storage.lastBookId = null

        val restored = controller.adoptActiveSpaceAfterSwitch()

        assertEquals(false, restored)
        assertEquals(null, controller.currentBook.value)
        assertEquals(null, controller.currentChapter.value)
        assertTrue(controller.chapters.value.isEmpty())
    }

    @Test
    fun adoptActiveSpaceAfterSwitchRestoresLastBook() = runBlocking {
        val book = testBook()
        val chapter1 = testChapter(id = 1, bookId = book.id, title = "001", durationMs = 60_000, sort = 0)
        val storage = FakeStorage(
            lastBookId = book.id,
            position = AudiobookPlaybackStorage.PlaybackPosition(chapter1.id, 10_000),
        )
        val controller = AudiobookPlaybackControllerImpl(
            player = FakePlaybackPlayer(initialState = PlaybackPlayer.State.Paused, positionMs = 10_000),
            catalog = FakeCatalog(book, listOf(chapter1)),
            storage = storage,
            speedPreferences = AudiobookPlaybackSpeedPreferences(FakePreferences()),
            librarySpaceRepository = FakeSpaceRepository(),
            scope = TestCoreScope(),
        )

        waitForCondition { controller.currentBook.value?.id == book.id }

        val restored = controller.adoptActiveSpaceAfterSwitch()

        assertTrue(restored)
        assertEquals(book.id, controller.currentBook.value?.id)
        assertEquals(chapter1.id, controller.currentChapter.value?.id)
    }

    @Test
    fun playBookWaitsForSpaceInitBeforePersistingPosition() = runBlocking {
        val book = testBook()
        val chapter1 = testChapter(id = 1, bookId = book.id, title = "001", durationMs = 60_000, sort = 0)
        val realSpaceId = LibrarySpace.Id(1)
        val spaceRepository = FakeSpaceRepository(
            initialSpaceId = LibrarySpace.Id(0),
            resolvedSpaceId = realSpaceId,
        )
        val storage = FakeStorage(lastBookId = null, position = null)
        val player = FakePlaybackPlayer(initialState = PlaybackPlayer.State.Paused, positionMs = 5_000)
        val controller = AudiobookPlaybackControllerImpl(
            player = player,
            catalog = FakeCatalog(book, listOf(chapter1)),
            storage = storage,
            speedPreferences = AudiobookPlaybackSpeedPreferences(FakePreferences()),
            librarySpaceRepository = spaceRepository,
            scope = TestCoreScope(),
        )

        controller.playBook(book)
        waitForCondition { storage.lastSavePositionSpaceId != null }

        assertEquals(realSpaceId, storage.lastSavePositionSpaceId)
        assertEquals(realSpaceId, storage.lastSaveLastPlayedSpaceId)
        assertTrue(spaceRepository.ensureInitializedCalls > 0)
    }

    private suspend fun waitForCondition(timeoutMs: Long = 1_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) {
            delay(10)
        }
        assertEquals(true, condition())
    }

    private fun testBook() = Book(
        id = Book.Id(1),
        title = "Test Book",
        folderUri = "content://folder",
        chapterCount = 2,
        subPath = "",
        totalDurationMs = 120_000,
    )

    private fun testChapter(
        id: Long,
        bookId: Book.Id,
        title: String,
        durationMs: Long,
        sort: Int,
    ) = Chapter(
        id = Chapter.Id(id),
        bookId = bookId,
        title = title,
        fileUri = "content://chapter/$id",
        duration = durationMs,
        sortOrder = sort,
    )

    private class TestCoreScope : CoreScope, CoroutineScope by CoroutineScope(Job() + Dispatchers.Unconfined)

    private class FakePreferences : Preferences {
        override fun saveBoolean(key: String, value: Boolean) = Unit
        override fun saveBooleans(vararg values: Pair<String, Boolean>) = Unit
        override fun loadBoolean(key: String, default: Boolean): Boolean = default
        override fun saveString(key: String, value: String?) = Unit
        override fun loadString(key: String, default: String?): String? = default
        override fun saveInt(key: String, value: Int?) = Unit
        override fun loadInt(key: String, default: Int): Int = default
        override fun saveLong(key: String, value: Long?) = Unit
        override fun loadLong(key: String, default: Long): Long = default
    }

    private class FakeStorage(
        var lastBookId: Book.Id?,
        private var position: AudiobookPlaybackStorage.PlaybackPosition?,
    ) : AudiobookPlaybackStorage {
        var lastSavePositionSpaceId: LibrarySpace.Id? = null
            private set
        var lastSaveLastPlayedSpaceId: LibrarySpace.Id? = null
            private set

        override suspend fun savePosition(
            spaceId: LibrarySpace.Id,
            bookId: Book.Id,
            chapterId: Chapter.Id,
            positionMs: Long,
        ) {
            lastSavePositionSpaceId = spaceId
            position = AudiobookPlaybackStorage.PlaybackPosition(chapterId, positionMs)
        }

        override suspend fun getPosition(
            spaceId: LibrarySpace.Id,
            bookId: Book.Id,
        ): AudiobookPlaybackStorage.PlaybackPosition? = position

        override suspend fun saveBookProgress(
            spaceId: LibrarySpace.Id,
            bookId: Book.Id,
            listenedDurationMs: Long,
            isCompleted: Boolean,
        ) = Unit

        override suspend fun saveLastPlayedBook(spaceId: LibrarySpace.Id, bookId: Book.Id) {
            lastSaveLastPlayedSpaceId = spaceId
        }

        override suspend fun getLastPlayedBookId(spaceId: LibrarySpace.Id): Book.Id? = lastBookId
    }

    private class FakeSpaceRepository(
        initialSpaceId: LibrarySpace.Id = LibrarySpace.Id(1),
        private val resolvedSpaceId: LibrarySpace.Id = initialSpaceId,
    ) : LibrarySpaceRepository {
        private fun space(id: LibrarySpace.Id) = LibrarySpace(
            id = id,
            name = LibrarySpace.DEFAULT_NAME,
            icon = LibrarySpace.DEFAULT_ICON,
            sortOrder = 0,
            isDefault = true,
        )

        override val activeSpaceId = MutableStateFlow(initialSpaceId)
        override val spaces = MutableStateFlow(
            if (initialSpaceId.value == 0L) emptyList() else listOf(space(initialSpaceId)),
        )
        override val activeSpace = MutableStateFlow(
            if (initialSpaceId.value == 0L) null else space(initialSpaceId),
        )

        var ensureInitializedCalls: Int = 0
            private set

        override suspend fun ensureInitialized() {
            ensureInitializedCalls++
            delay(50)
            val resolved = space(resolvedSpaceId)
            spaces.value = listOf(resolved)
            activeSpaceId.value = resolvedSpaceId
            activeSpace.value = resolved
        }

        override fun setActiveSpace(id: LibrarySpace.Id) = Unit
        override suspend fun refreshSpaces() = Unit
        override suspend fun createSpace(name: String, icon: String): LibrarySpace.Id? = null
        override suspend fun renameSpace(id: LibrarySpace.Id, name: String, icon: String) = Unit
        override suspend fun deleteSpace(id: LibrarySpace.Id) = Unit
        override suspend fun addBooks(spaceId: LibrarySpace.Id, bookIds: List<Book.Id>) = Unit
        override suspend fun addBooksBySubPath(spaceId: LibrarySpace.Id, subPath: String) = Unit
        override suspend fun removeBook(spaceId: LibrarySpace.Id, bookId: Book.Id) = Unit
        override suspend fun removeBooksBySubPath(spaceId: LibrarySpace.Id, subPath: String) = Unit
        override suspend fun getBooksGlobal(): List<Book> = emptyList()
    }

    private class FakeCatalog(
        private val book: Book,
        private val chapters: List<Chapter>,
    ) : AudiobookCatalogSource {
        override val books: Flow<List<Book>> = flowOf(listOf(book))
        override val continueListeningBooks: Flow<List<Book>> = flowOf(listOf(book))
        override val folderSources: Flow<List<FolderSource>> = flowOf(emptyList())
        override val catalogScanUi: StateFlow<CatalogScanUi> =
            MutableStateFlow(CatalogScanUi(detail = CatalogScanDetail.Preparing))

        override suspend fun addFolderAndScan(uri: String, name: String) = Unit
        override suspend fun removeFolder(id: FolderSource.Id) = Unit
        override suspend fun rescanAllFolders() = Unit
        override suspend fun getFolderSourcesList(): List<FolderSource> = emptyList()
        override suspend fun countBooksByFolderSource(folderSourceId: FolderSource.Id): Int = 0
        override suspend fun diagnoseFolderAccess(folder: FolderSource): FolderSourceAccessHealth =
            FolderSourceAccessHealth.Ok
        override suspend fun getBooks(): List<Book> = listOf(book)
        override suspend fun getBook(bookId: Book.Id): Book? = book.takeIf { it.id == bookId }
        override suspend fun getChapters(bookId: Book.Id): List<Chapter> = chapters
        override suspend fun setHiddenFromContinueListening(bookId: Book.Id, hidden: Boolean) = Unit
        override suspend fun updateBookCoverUriIfEmpty(bookId: Book.Id, coverUri: String) = Unit
    }

    private class FakePlaybackPlayer(
        initialState: PlaybackPlayer.State,
        positionMs: Long,
    ) : PlaybackPlayer {
        private val stateFlow = MutableStateFlow(initialState)
        override val state: StateFlow<PlaybackPlayer.State> = stateFlow.asStateFlow()
        override val progress: Flow<PlaybackPlayer.Progress> =
            flowOf(PlaybackPlayer.Progress(position = positionMs, duration = 60_000))
        private val _playbackSpeed = MutableStateFlow(1f)
        override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

        private var position = positionMs
        private var duration = 60_000L

        var onSetMediaItem: (() -> Unit)? = null

        var pauseCalls: Int = 0
            private set
        var resumeCalls: Int = 0
            private set

        fun resetCallCounts() {
            pauseCalls = 0
            resumeCalls = 0
        }

        fun setState(state: PlaybackPlayer.State) {
            stateFlow.value = state
        }

        override suspend fun stop() = Unit

        override suspend fun pause() {
            pauseCalls++
            stateFlow.value = PlaybackPlayer.State.Paused
        }

        override suspend fun resume() {
            resumeCalls++
            stateFlow.value = PlaybackPlayer.State.Playing
        }

        override suspend fun seekTo(position: Long) {
            this.position = position
        }

        override suspend fun currentProgress(): PlaybackPlayer.Progress {
            yield()
            return PlaybackPlayer.Progress(position, duration)
        }

        override suspend fun setMediaItem(item: MediaItemWrapper, position: Long) {
            onSetMediaItem?.invoke()
            onSetMediaItem = null
            yield()
            this.position = position
            duration = 60_000L
            stateFlow.value = PlaybackPlayer.State.Paused
        }

        override suspend fun setPlaybackSpeed(speed: Float) {
            _playbackSpeed.value = speed
        }
    }
}
