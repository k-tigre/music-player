package by.tigre.audiobook.core.data.audiobook_playback

import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import kotlinx.coroutines.flow.StateFlow

interface AudiobookPlaybackController {

    val player: PlaybackPlayer
    val currentBook: StateFlow<Book?>
    val currentChapter: StateFlow<Chapter?>

    fun playBook(book: Book)
    fun playNextChapter()
    fun playPrevChapter()
    fun pause()
    fun resume()
    fun stop()
}
