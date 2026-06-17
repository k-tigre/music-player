package by.tigre.audiobook.car

import android.net.Uri
import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.media.platform.background.car.CarBrowseItem
import by.tigre.media.platform.background.car.CarMediaIds
import by.tigre.media.platform.background.car.CarMediaLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AudiobookCarMediaLibrary(
    private val scope: CoroutineScope,
    private val catalog: AudiobookCatalogSource,
    private val playback: AudiobookPlaybackController,
    private val booksTabTitle: String,
) : CarMediaLibrary {

    override suspend fun getChildren(parentId: String): List<CarBrowseItem> = when (parentId) {
        CarMediaIds.ROOT -> listOf(
            CarBrowseItem(
                id = CarMediaIds.TAB_BOOKS,
                title = booksTabTitle,
                isBrowsable = true,
                isPlayable = false,
            )
        )
        CarMediaIds.TAB_BOOKS -> catalog.getBooks().map { book -> bookItem(book, browsable = true) }
        else -> {
            val bookId = CarMediaIds.parseBookId(parentId) ?: return emptyList()
            catalog.getChapters(Book.Id(bookId)).map { chapter -> chapterItem(bookId, chapter) }
        }
    }

    override fun playMediaId(mediaId: String) {
        CarMediaIds.parseChapterIds(mediaId)?.let { (bookId, chapterId) ->
            playback.playBookChapter(Book.Id(bookId), Chapter.Id(chapterId))
            return
        }
        CarMediaIds.parseBookId(mediaId)?.let { bookId ->
            scope.launch {
                catalog.getBook(Book.Id(bookId))?.let { playback.playBook(it) }
            }
        }
    }

    override suspend fun getBrowseItem(mediaId: String): CarBrowseItem? {
        CarMediaIds.parseChapterIds(mediaId)?.let { (bookId, chapterId) ->
            val chapter = catalog.getChapters(Book.Id(bookId))
                .firstOrNull { it.id.value == chapterId } ?: return null
            return chapterItem(bookId, chapter)
        }
        CarMediaIds.parseBookId(mediaId)?.let { bookId ->
            val book = catalog.getBook(Book.Id(bookId)) ?: return null
            return bookItem(book, browsable = true)
        }
        return null
    }

    private fun bookItem(book: Book, browsable: Boolean): CarBrowseItem = CarBrowseItem(
        id = CarMediaIds.book(book.id.value),
        title = book.title,
        subtitle = book.chapterCount.takeIf { it > 0 }?.let { "$it chapters" },
        isBrowsable = browsable,
        isPlayable = !browsable,
        artworkUri = book.coverUri?.let(Uri::parse),
    )

    private fun chapterItem(bookId: Long, chapter: Chapter): CarBrowseItem = CarBrowseItem(
        id = CarMediaIds.chapter(bookId, chapter.id.value),
        title = chapter.title,
        isBrowsable = false,
        isPlayable = true,
    )
}
