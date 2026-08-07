package by.tigre.audiobook.core.data.storage.audiobook_playback

import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.audiobook.core.entity.catalog.LibrarySpace

interface AudiobookPlaybackStorage {

    suspend fun savePosition(
        spaceId: LibrarySpace.Id,
        bookId: Book.Id,
        chapterId: Chapter.Id,
        positionMs: Long,
    )

    suspend fun getPosition(spaceId: LibrarySpace.Id, bookId: Book.Id): PlaybackPosition?

    suspend fun saveBookProgress(
        spaceId: LibrarySpace.Id,
        bookId: Book.Id,
        listenedDurationMs: Long,
        isCompleted: Boolean,
    )

    suspend fun saveLastPlayedBook(spaceId: LibrarySpace.Id, bookId: Book.Id)
    suspend fun getLastPlayedBookId(spaceId: LibrarySpace.Id): Book.Id?

    data class PlaybackPosition(val chapterId: Chapter.Id, val positionMs: Long)
}
