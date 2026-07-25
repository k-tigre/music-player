package by.tigre.audiobook.core.data.audiobook_playback.impl

import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook.CatalogScanDetail
import by.tigre.audiobook.core.data.audiobook.CatalogScanUi
import by.tigre.audiobook.core.data.audiobook.FolderSourceAccessHealth
import by.tigre.audiobook.core.data.audiobook_playback.prefs.AudiobookPlaybackSpeedPreferences
import by.tigre.audiobook.core.data.storage.audiobook_playback.AudiobookPlaybackStorage
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.audiobook.core.entity.catalog.FolderSource
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
            scope = TestCoreScope(),
        )

        waitForCondition { controller.currentChapter.value?.id == chapter1.id }
        controller.resume()
        waitForCondition { player.resumeCalls > 0 }
        player.resetCallCounts()

        controller.pause()
        waitForCondition { player.pauseCalls > 0 }

        assertEquals(1, player.pauseCalls)
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
        private val lastBookId: Book.Id?,
        private var position: AudiobookPlaybackStorage.PlaybackPosition?,
    ) : AudiobookPlaybackStorage {
        override suspend fun savePosition(bookId: Book.Id, chapterId: Chapter.Id, positionMs: Long) {
            position = AudiobookPlaybackStorage.PlaybackPosition(chapterId, positionMs)
        }

        override suspend fun getPosition(bookId: Book.Id): AudiobookPlaybackStorage.PlaybackPosition? = position

        override suspend fun saveBookProgress(bookId: Book.Id, listenedDurationMs: Long, isCompleted: Boolean) = Unit

        override suspend fun saveLastPlayedBook(bookId: Book.Id) = Unit

        override suspend fun getLastPlayedBookId(): Book.Id? = lastBookId
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
        override suspend fun diagnoseFolderAccess(folder: FolderSource): FolderSourceAccessHealth =
            FolderSourceAccessHealth.Ok
        override suspend fun getBooks(): List<Book> = listOf(book)
        override suspend fun getBook(bookId: Book.Id): Book? = book.takeIf { it.id == bookId }
        override suspend fun getChapters(bookId: Book.Id): List<Chapter> = chapters
        override suspend fun setHiddenFromContinueListening(bookId: Book.Id, hidden: Boolean) = Unit
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
