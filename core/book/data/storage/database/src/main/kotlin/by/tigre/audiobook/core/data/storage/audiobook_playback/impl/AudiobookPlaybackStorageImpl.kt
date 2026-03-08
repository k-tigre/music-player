package by.tigre.audiobook.core.data.storage.audiobook_playback.impl

import by.tigre.audiobook.core.data.storage.audiobook.DatabaseAudiobook
import by.tigre.audiobook.core.data.storage.audiobook_playback.AudiobookPlaybackStorage
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter

internal class AudiobookPlaybackStorageImpl(
    private val database: DatabaseAudiobook
) : AudiobookPlaybackStorage {

    override suspend fun savePosition(bookId: Book.Id, chapterId: Chapter.Id, positionMs: Long) {
        database.audiobookPlaybackQueries.upsertPosition(
            book_id = bookId.value,
            chapter_id = chapterId.value,
            position_ms = positionMs
        )
    }

    override suspend fun getPosition(bookId: Book.Id): AudiobookPlaybackStorage.PlaybackPosition? {
        return database.audiobookPlaybackQueries.getPosition(book_id = bookId.value) { chapterId, positionMs ->
            AudiobookPlaybackStorage.PlaybackPosition(
                chapterId = Chapter.Id(chapterId),
                positionMs = positionMs
            )
        }.executeAsOneOrNull()
    }

    override suspend fun saveBookProgress(
        bookId: Book.Id,
        listenedDurationMs: Long,
        isCompleted: Boolean
    ) {
        database.bookQueries.upsertProgress(
            listened_duration_ms = listenedDurationMs,
            is_completed = if (isCompleted) 1L else 0L,
            id = bookId.value
        )
    }

    override suspend fun saveLastPlayedBook(bookId: Book.Id) {
        database.audiobookPlaybackQueries.upsertLastPlayed(book_id = bookId.value)
    }

    override suspend fun getLastPlayedBookId(): Book.Id? {
        return database.audiobookPlaybackQueries.getLastPlayed()
            .executeAsOneOrNull()
            ?.let { Book.Id(it) }
    }
}
