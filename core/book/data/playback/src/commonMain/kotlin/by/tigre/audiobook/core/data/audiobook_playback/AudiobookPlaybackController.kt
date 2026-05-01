package by.tigre.audiobook.core.data.audiobook_playback

import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import kotlinx.coroutines.flow.StateFlow

interface AudiobookPlaybackController {

    val player: PlaybackPlayer
    val currentBook: StateFlow<Book?>
    val currentChapter: StateFlow<Chapter?>

    fun loadBook(book: Book)
    fun playBook(book: Book)
    fun playNextChapter()
    fun playPrevChapter()
    fun pause()
    fun resume()
    fun stop()

    /** Persists chapter/file position after the user scrubbed the timeline (e.g. while paused). */
    fun persistPlaybackPositionAfterSeek(positionMs: Long)

    /**
     * Pauses playback and optionally seeks back in the current chapter (e.g. night sleep timer).
     * Does not apply the “rewind after pause” logic used for normal pause/resume.
     */
    suspend fun endPlaybackForNightTimer(rewindMs: Long?)
}
