package by.tigre.audiobook.core.data.storage.audiobook_playback

import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter

interface AudiobookPlaybackStorage {

    suspend fun savePosition(bookId: Book.Id, chapterId: Chapter.Id, positionMs: Long)

    suspend fun getPosition(bookId: Book.Id): PlaybackPosition?

    suspend fun saveBookProgress(bookId: Book.Id, listenedDurationMs: Long, isCompleted: Boolean)

    data class PlaybackPosition(val chapterId: Chapter.Id, val positionMs: Long)
}
