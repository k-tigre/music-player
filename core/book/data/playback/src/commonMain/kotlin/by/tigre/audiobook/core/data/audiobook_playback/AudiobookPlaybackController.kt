package by.tigre.audiobook.core.data.audiobook_playback

import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.media.platform.playback.PlaybackPlayer
import kotlinx.coroutines.flow.StateFlow

interface AudiobookPlaybackController {

    val player: PlaybackPlayer
    val currentBook: StateFlow<Book?>
    val currentChapter: StateFlow<Chapter?>
    val chapters: StateFlow<List<Chapter>>
    /** True after the book ends; cleared when the user seeks back or starts playback again. */
    val bookFinishedBannerVisible: StateFlow<Boolean>

    fun loadBook(book: Book)
    fun playBook(book: Book)
    fun playBookChapter(bookId: Book.Id, chapterId: Chapter.Id)
    fun playNextChapter()
    fun playPrevChapter()
    fun jumpToChapter(chapterId: Chapter.Id)
    fun pause()
    fun resume()
    fun stop()
    fun seekBy(deltaMs: Long)

    /** Persists chapter/file position after the user scrubbed the timeline (e.g. while paused). */
    fun persistPlaybackPositionAfterSeek(positionMs: Long)

    /**
     * Pauses playback and optionally seeks back in the current chapter (e.g. night sleep timer).
     * Does not apply the “rewind after pause” logic used for normal pause/resume.
     */
    suspend fun endPlaybackForNightTimer(rewindMs: Long?)
}
