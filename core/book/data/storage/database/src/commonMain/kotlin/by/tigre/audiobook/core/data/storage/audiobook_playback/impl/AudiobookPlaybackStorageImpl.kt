package by.tigre.audiobook.core.data.storage.audiobook_playback.impl

import by.tigre.audiobook.core.data.storage.audiobook.DatabaseAudiobook
import by.tigre.audiobook.core.data.storage.audiobook_playback.AudiobookPlaybackStorage
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.audiobook.core.entity.catalog.LibrarySpace

internal class AudiobookPlaybackStorageImpl(
    private val database: DatabaseAudiobook
) : AudiobookPlaybackStorage {

    override suspend fun savePosition(
        spaceId: LibrarySpace.Id,
        bookId: Book.Id,
        chapterId: Chapter.Id,
        positionMs: Long,
    ) {
        database.audiobookPlaybackQueries.upsertSpacePosition(
            space_id = spaceId.value,
            book_id = bookId.value,
            chapter_id = chapterId.value,
            position_ms = positionMs,
        )
    }

    override suspend fun getPosition(
        spaceId: LibrarySpace.Id,
        bookId: Book.Id,
    ): AudiobookPlaybackStorage.PlaybackPosition? {
        return database.audiobookPlaybackQueries.getSpacePosition(
            space_id = spaceId.value,
            book_id = bookId.value,
        ) { chapterId, positionMs ->
            AudiobookPlaybackStorage.PlaybackPosition(
                chapterId = Chapter.Id(chapterId),
                positionMs = positionMs,
            )
        }.executeAsOneOrNull()
    }

    override suspend fun saveBookProgress(
        spaceId: LibrarySpace.Id,
        bookId: Book.Id,
        listenedDurationMs: Long,
        isCompleted: Boolean,
    ) {
        val existing = database.librarySpaceQueries.getSpaceProgress(
            space_id = spaceId.value,
            book_id = bookId.value,
        ).executeAsOneOrNull()
        val hidden = existing?.hidden_from_continue ?: 0L
        database.librarySpaceQueries.upsertSpaceProgress(
            space_id = spaceId.value,
            book_id = bookId.value,
            listened_duration_ms = listenedDurationMs,
            is_completed = if (isCompleted) 1L else 0L,
            hidden_from_continue = hidden,
        )
    }

    override suspend fun saveLastPlayedBook(spaceId: LibrarySpace.Id, bookId: Book.Id) {
        database.audiobookPlaybackQueries.upsertSpaceLastPlayed(
            space_id = spaceId.value,
            book_id = bookId.value,
        )
        database.audiobookPlaybackQueries.upsertSpaceRecentBook(
            space_id = spaceId.value,
            book_id = bookId.value,
            last_started_at = System.currentTimeMillis(),
        )
    }

    override suspend fun getLastPlayedBookId(spaceId: LibrarySpace.Id): Book.Id? {
        return database.audiobookPlaybackQueries.getSpaceLastPlayed(space_id = spaceId.value)
            .executeAsOneOrNull()
            ?.let { Book.Id(it) }
    }
}
