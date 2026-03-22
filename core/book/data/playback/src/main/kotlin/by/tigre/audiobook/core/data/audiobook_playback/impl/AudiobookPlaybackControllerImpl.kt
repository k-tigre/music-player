package by.tigre.audiobook.core.data.audiobook_playback.impl

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackConfig
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.data.storage.audiobook_playback.AudiobookPlaybackStorage
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.logger.Log
import by.tigre.music.player.tools.coroutines.CoreScope
import by.tigre.music.player.tools.coroutines.extensions.tickerFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@OptIn(ExperimentalCoroutinesApi::class)
internal class AudiobookPlaybackControllerImpl(
    override val player: PlaybackPlayer,
    private val catalog: AudiobookCatalogSource,
    private val storage: AudiobookPlaybackStorage,
    private val scope: CoreScope
) : AudiobookPlaybackController {

    override val currentBook = MutableStateFlow<Book?>(null)
    override val currentChapter = MutableStateFlow<Chapter?>(null)

    private val chapters = MutableStateFlow<List<Chapter>>(emptyList())
    private val isPlaying = MutableStateFlow(false)

    /** Monotonic mark when [pause] was invoked; used to rewind on [resume]. */
    private var pauseStartedAt: TimeMark? = null

    /** True after [pause] until [resume] or [clearPauseRewindState]; avoids rewinding on first [resume] without pause. */
    private var shouldRewindOnResume: Boolean = false

    /**
     * Listened time (ms) in the book at the stored position when the book was loaded; playback may start rewound from here
     * without persisting until [mayPersistBelowCanonical] or until caught up.
     */
    private var loadCanonicalListenedMs: Long? = null

    /** After user paused while playing, allow saving a position earlier than [loadCanonicalListenedMs] (e.g. resume rewind). */
    private var mayPersistBelowCanonical: Boolean = false

    init {
        scope.launch {
            player.state
                .filter { it == PlaybackPlayer.State.Ended }
                .collect { playNextChapter() }
        }

        scope.launch {
            isPlaying
                .flatMapLatest { isPlaying ->
                    if (isPlaying) {
                        tickerFlow(15.seconds, 1.seconds)
                    } else {
                        emptyFlow()
                    }
                }.collect {
                    saveCurrentPosition()
                }
        }

        scope.launch {
            restoreLastPlayedBook()
        }
    }

    override fun loadBook(book: Book) {
        Log.d(TAG) { "loadBook: ${book.title}" }
        scope.launch { loadBookInternal(book, autoPlay = false) }
    }

    override fun playBook(book: Book) {
        Log.d(TAG) { "playBook: ${book.title}" }
        scope.launch {
            loadBookInternal(book, autoPlay = true)
            saveCurrentPosition()
        }
    }

    private suspend fun loadBookInternal(book: Book, autoPlay: Boolean) {
        clearPauseRewindState()
        val chapterList = catalog.getChapters(book.id)
        if (chapterList.isEmpty()) {
            Log.w(TAG) { "No chapters for book: ${book.title}" }
            return
        }

        chapters.value = chapterList
        currentBook.value = book

        val savedPosition = storage.getPosition(book.id)
        val startChapter = if (savedPosition != null) {
            chapterList.firstOrNull { it.id == savedPosition.chapterId } ?: chapterList.first()
        } else {
            chapterList.first()
        }
        val startPosition = if (savedPosition?.chapterId == startChapter.id) savedPosition.positionMs else 0L

        setChapter(startChapter, startPosition)
        loadCanonicalListenedMs = listenedMsForChapterPosition(chapterList, startChapter, startPosition)
        mayPersistBelowCanonical = false
        rewindAcrossChapters(AudiobookPlaybackConfig.RESUME_REWIND_MAX_MS)
        storage.saveLastPlayedBook(book.id)

        if (autoPlay) {
            isPlaying.value = true
            player.resume()
        }
    }

    private suspend fun restoreLastPlayedBook() {
        val bookId = storage.getLastPlayedBookId() ?: return
        val book = catalog.getBook(bookId) ?: return
        Log.d(TAG) { "Restoring last played book: ${book.title}" }
        loadBookInternal(book, autoPlay = false)
    }

    override fun playNextChapter() {
        Log.d(TAG) { "playNextChapter" }
        scope.launch {
            clearPauseRewindState()
            loadCanonicalListenedMs = null
            saveCurrentPosition()
            val chapterList = chapters.value
            val current = currentChapter.value ?: return@launch
            val nextIndex = chapterList.indexOfFirst { it.id == current.id } + 1
            if (nextIndex < chapterList.size) {
                setChapter(chapterList[nextIndex], 0L)
                player.resume()
            } else {
                Log.d(TAG) { "Book finished" }
                currentBook.value?.let { book ->
                    storage.savePosition(book.id, chapterList.first().id, 0L)
                    saveBookProgressCompleted(book, chapterList)
                }
                isPlaying.value = false
                player.stop()
            }
        }
    }

    override fun playPrevChapter() {
        Log.d(TAG) { "playPrevChapter" }
        scope.launch {
            clearPauseRewindState()
            loadCanonicalListenedMs = null
            saveCurrentPosition()
            val chapterList = chapters.value
            val current = currentChapter.value ?: return@launch
            val prevIndex = chapterList.indexOfFirst { it.id == current.id } - 1
            if (prevIndex >= 0) {
                setChapter(chapterList[prevIndex], 0L)
                player.resume()
            } else {
                setChapter(chapterList.first(), 0L)
                player.resume()
            }
        }
    }

    override fun pause() {
        Log.d(TAG) { "pause" }
        val wasPlaying = isPlaying.value
        shouldRewindOnResume = true
        pauseStartedAt = TimeSource.Monotonic.markNow()
        scope.launch {
            if (wasPlaying) {
                mayPersistBelowCanonical = true
            }
            saveCurrentPosition()
            isPlaying.value = false
            player.pause()
        }
    }

    override fun resume() {
        Log.d(TAG) { "resume" }
        isPlaying.value = true
        val wantsRewind = shouldRewindOnResume
        shouldRewindOnResume = false
        val pauseMark = pauseStartedAt
        pauseStartedAt = null
        scope.launch {
            val rewindMs = rewindMsAfterPause(wantsRewind, pauseMark)
            if (rewindMs > 0L) {
                rewindAcrossChapters(rewindMs)
            }
            saveCurrentPosition()
            player.resume()
        }
    }

    override fun stop() {
        Log.d(TAG) { "stop" }
        clearPauseRewindState()
        loadCanonicalListenedMs = null
        scope.launch {
            saveCurrentPosition()
            isPlaying.value = false
            player.stop()
        }
    }

    private suspend fun setChapter(chapter: Chapter, positionMs: Long) {
        currentChapter.value = chapter
        withContext(Dispatchers.Main) {
            player.player.playWhenReady = false
            player.player.setMediaItem(
                MediaItem.Builder()
                    .setUri(chapter.fileUri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(chapter.title)
                            .setAlbumTitle(currentBook.value?.title)
                            .build()
                    )
                    .build(),
                positionMs
            )
            player.player.prepare()
        }
    }

    private suspend fun saveCurrentPosition() {
        val book = currentBook.value ?: return
        val chapter = currentChapter.value ?: return
        val progress = player.progress.first()
        val chapterList = chapters.value
        val canonical = loadCanonicalListenedMs
        if (canonical != null && !mayPersistBelowCanonical) {
            val currentListened = listenedMsForChapterPosition(chapterList, chapter, progress.position)
            if (currentListened < canonical) {
                return
            }
        }
        if (progress.position > 0) {
            storage.savePosition(book.id, chapter.id, progress.position)
            Log.d(TAG) { "Saved position: book=${book.title}, chapter=${chapter.title}, pos=${progress.position}" }
        }
        saveBookProgress(book, chapter, progress.position)
    }

    private suspend fun saveBookProgress(book: Book, currentChapter: Chapter, currentPositionMs: Long) {
        val chapterList = chapters.value
        if (chapterList.isEmpty()) return

        val currentIndex = chapterList.indexOfFirst { it.id == currentChapter.id }
        if (currentIndex < 0) return

        val listenedDurationMs = chapterList.take(currentIndex).sumOf { it.duration } + currentPositionMs

        val isLastChapter = currentIndex == chapterList.size - 1
        val remainingInChapter = (currentChapter.duration - currentPositionMs).coerceAtLeast(0)
        val isCompleted = isLastChapter && remainingInChapter < AudiobookPlaybackConfig.BOOK_COMPLETION_THRESHOLD_MS

        storage.saveBookProgress(book.id, listenedDurationMs, isCompleted)
        Log.d(TAG) { "Saved book progress: book=${book.title}, listened=$listenedDurationMs, completed=$isCompleted" }
    }

    private fun clearPauseRewindState() {
        pauseStartedAt = null
        shouldRewindOnResume = false
    }

    private fun listenedMsForChapterPosition(
        chapterList: List<Chapter>,
        chapter: Chapter,
        positionMs: Long,
    ): Long {
        val idx = chapterList.indexOfFirst { it.id == chapter.id }
        if (idx < 0) return 0L
        return chapterList.take(idx).sumOf { it.duration } + positionMs.coerceAtLeast(0L)
    }

    private fun rewindMsAfterPause(wantsRewind: Boolean, pauseMark: TimeMark?): Long {
        if (!wantsRewind) return 0L
        if (pauseMark != null) {
            return rewindMsForPauseDuration(pauseMark.elapsedNow())
        }
        return AudiobookPlaybackConfig.RESUME_REWIND_WHEN_PAUSE_UNKNOWN_MS
    }

    private fun rewindMsForPauseDuration(pausedFor: Duration): Long {
        val minMs = AudiobookPlaybackConfig.RESUME_REWIND_MIN_MS
        val maxMs = AudiobookPlaybackConfig.RESUME_REWIND_MAX_MS
        val rampMs = AudiobookPlaybackConfig.RESUME_REWIND_RAMP_MS.toDouble().coerceAtLeast(1.0)
        val elapsedMs = pausedFor.inWholeMilliseconds
        val fraction = (elapsedMs / rampMs).coerceIn(0.0, 1.0)
        return (minMs + (maxMs - minMs) * fraction).toLong()
    }

    /**
     * Moves playback back by [rewindMs] within the current chapter; overflow goes to the previous chapter(s)
     * from the end of each file, same as rewinding from the chapter boundary.
     */
    private suspend fun rewindAcrossChapters(rewindMs: Long) {
        val chapterList = chapters.value
        val current = currentChapter.value ?: return
        var chapterIndex = chapterList.indexOfFirst { it.id == current.id }
        if (chapterIndex < 0) return

        val positionMs = player.progress.first().position.coerceAtLeast(0L)
        var remaining = rewindMs

        if (positionMs >= remaining) {
            player.seekTo(positionMs - remaining)
            return
        }

        remaining -= positionMs
        chapterIndex--

        while (chapterIndex >= 0 && remaining > 0) {
            val chapter = chapterList[chapterIndex]
            val durationMs = chapter.duration.coerceAtLeast(0L)
            if (durationMs == 0L) {
                chapterIndex--
                continue
            }
            if (remaining <= durationMs) {
                val targetPos = (durationMs - remaining).coerceAtLeast(0L)
                setChapter(chapter, targetPos)
                return
            }
            remaining -= durationMs
            chapterIndex--
        }

        setChapter(chapterList.first(), 0L)
    }

    private suspend fun saveBookProgressCompleted(book: Book, chapterList: List<Chapter>) {
        val totalDurationMs = chapterList.sumOf { it.duration }
        storage.saveBookProgress(book.id, totalDurationMs, isCompleted = true)
        Log.d(TAG) { "Marked book as completed: ${book.title}" }
    }

    private companion object {
        const val TAG = "AudiobookPlaybackController"
    }
}
