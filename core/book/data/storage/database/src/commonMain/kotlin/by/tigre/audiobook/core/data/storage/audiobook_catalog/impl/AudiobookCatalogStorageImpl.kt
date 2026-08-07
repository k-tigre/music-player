package by.tigre.audiobook.core.data.storage.audiobook_catalog.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import by.tigre.audiobook.core.data.storage.audiobook.DatabaseAudiobook
import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage
import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage.ScannedBook
import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage.ScannedChapter
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.audiobook.core.entity.catalog.FolderSource
import by.tigre.audiobook.core.entity.catalog.LibrarySpace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import audiobook.Chapter as ChapterRow

class AudiobookCatalogStorageImpl(
    private val database: DatabaseAudiobook,
    private val scope: CoroutineScope,
) : AudiobookCatalogStorage {

    override fun observeBooksInSpace(spaceId: LibrarySpace.Id): Flow<List<Book>> =
        database.bookQueries.selectAllInSpace(spaceId.value, spaceId.value, ::mapBookWithProgress)
            .asFlow()
            .mapToList(scope.coroutineContext)

    override fun observeContinueListeningInSpace(spaceId: LibrarySpace.Id): Flow<List<Book>> =
        database.bookQueries.selectRecentContinueListening(spaceId.value, ::mapBookWithProgress)
            .asFlow()
            .mapToList(scope.coroutineContext)

    override val folderSources: Flow<List<FolderSource>> =
        database.folderSourceQueries.selectAll { id, uri, name ->
            FolderSource(id = FolderSource.Id(id), uri = uri, name = name)
        }.asFlow().mapToList(scope.coroutineContext)
            .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    override suspend fun addFolderSource(uri: String, name: String): FolderSource.Id {
        database.folderSourceQueries.insertFolder(uri, name)
        return FolderSource.Id(database.folderSourceQueries.lastInsertId().executeAsOne())
    }

    override suspend fun getFolderSources(): List<FolderSource> =
        database.folderSourceQueries.selectAll { id, uri, name ->
            FolderSource(id = FolderSource.Id(id), uri = uri, name = name)
        }.executeAsList()

    override suspend fun removeFolderSource(id: FolderSource.Id) {
        database.transactionWithResult {
            database.bookQueries.deleteByFolderSource(id.value)
            database.folderSourceQueries.deleteById(id.value)
        }
    }

    override suspend fun getFolderSourceByUri(uri: String): FolderSource? =
        database.folderSourceQueries.selectByUri(uri) { id, u, name ->
            FolderSource(id = FolderSource.Id(id), uri = u, name = name)
        }.executeAsOneOrNull()

    override suspend fun syncBooksForFolder(folderSourceId: FolderSource.Id, scannedBooks: List<ScannedBook>) {
        val defaultSpaceId = getDefaultSpaceId().value
        database.transaction {
            val existingBooks = database.bookQueries
                .selectBooksByFolderSourceId(folderSourceId.value)
                .executeAsList()

            val scannedByKey = scannedBooks.associateBy { it.folderUri to it.title }
            val existingByKey = existingBooks.associateBy { it.folder_uri to it.title }

            for (existing in existingBooks) {
                val key = existing.folder_uri to existing.title
                if (key !in scannedByKey) {
                    database.bookQueries.deleteById(existing.id)
                }
            }

            for (scanned in scannedBooks) {
                val key = scanned.folderUri to scanned.title
                val existing = existingByKey[key]

                val bookId: Long
                if (existing != null) {
                    bookId = existing.id
                    database.bookQueries.updateBookMetadata(
                        sub_path = scanned.subPath,
                        total_duration_ms = scanned.totalDurationMs,
                        cover_uri = scanned.coverUri,
                        id = bookId,
                    )
                } else {
                    database.bookQueries.insertBook(
                        scanned.title,
                        scanned.folderUri,
                        folderSourceId.value,
                        scanned.subPath,
                        scanned.totalDurationMs,
                        cover_uri = scanned.coverUri,
                    )
                    bookId = database.bookQueries.lastInsertId().executeAsOne()
                }

                database.librarySpaceQueries.insertBookInSpace(defaultSpaceId, bookId)

                val existingChapters = database.chapterQueries.selectByBook(bookId).executeAsList()
                val existingByUri = existingChapters.associateBy { it.file_uri }
                val scannedUris = scanned.chapters.map { it.fileUri }.toSet()

                for (row in existingChapters) {
                    if (row.file_uri !in scannedUris) {
                        database.chapterQueries.deleteChapterById(row.id)
                    }
                }

                for (chapter in scanned.chapters) {
                    val row = existingByUri[chapter.fileUri]
                    if (row == null) {
                        database.chapterQueries.insertChapter(
                            book_id = bookId,
                            title = chapter.title,
                            file_uri = chapter.fileUri,
                            duration = chapter.duration,
                            sort_order = chapter.sortOrder.toLong(),
                            source_size = chapter.sourceSize,
                            source_last_modified = chapter.sourceLastModified,
                        )
                    } else if (!chapterRowMatches(row, chapter)) {
                        database.chapterQueries.updateChapterById(
                            title = chapter.title,
                            duration = chapter.duration,
                            sort_order = chapter.sortOrder.toLong(),
                            source_size = chapter.sourceSize,
                            source_last_modified = chapter.sourceLastModified,
                            id = row.id,
                        )
                    }
                }
            }
        }
    }

    override suspend fun countBooksByFolderSource(folderSourceId: FolderSource.Id): Int =
        database.bookQueries.selectBooksByFolderSourceId(folderSourceId.value).executeAsList().size

    override suspend fun getExistingBookIdForScan(
        folderSourceId: FolderSource.Id,
        folderUri: String,
        title: String,
    ): Book.Id? =
        database.bookQueries
            .selectIdByFolderSourceFolderUriTitle(folderSourceId.value, folderUri, title)
            .executeAsOneOrNull()
            ?.let { Book.Id(it) }

    override suspend fun getBooksGlobal(): List<Book> =
        database.bookQueries.selectAllGlobal { id, title, folderUri, chapterCount, subPath, totalDurationMs, coverUri ->
            Book(
                id = Book.Id(id),
                title = title,
                folderUri = folderUri,
                chapterCount = chapterCount.toInt(),
                subPath = subPath,
                totalDurationMs = totalDurationMs,
                coverUri = coverUri,
            )
        }.executeAsList()

    override suspend fun getBooksInSpace(spaceId: LibrarySpace.Id): List<Book> =
        database.bookQueries.selectAllInSpace(spaceId.value, spaceId.value, ::mapBookWithProgress)
            .executeAsList()

    override suspend fun getBookInSpace(spaceId: LibrarySpace.Id, bookId: Book.Id): Book? =
        database.bookQueries.selectByIdInSpace(spaceId.value, bookId.value, ::mapBookWithProgress)
            .executeAsOneOrNull()

    override suspend fun getBookGlobal(bookId: Book.Id): Book? =
        database.bookQueries.selectByIdGlobal(bookId.value) { id, title, folderUri, chapterCount, subPath, totalDurationMs, coverUri ->
            Book(
                id = Book.Id(id),
                title = title,
                folderUri = folderUri,
                chapterCount = chapterCount.toInt(),
                subPath = subPath,
                totalDurationMs = totalDurationMs,
                coverUri = coverUri,
            )
        }.executeAsOneOrNull()

    override suspend fun setHiddenFromContinue(
        spaceId: LibrarySpace.Id,
        bookId: Book.Id,
        hidden: Boolean,
    ) {
        val existing = database.librarySpaceQueries.getSpaceProgress(
            space_id = spaceId.value,
            book_id = bookId.value,
        ).executeAsOneOrNull()
        database.librarySpaceQueries.setHiddenFromContinueInSpace(
            space_id = spaceId.value,
            book_id = bookId.value,
            listened_duration_ms = existing?.listened_duration_ms ?: 0L,
            is_completed = existing?.is_completed ?: 0L,
            hidden_from_continue = if (hidden) 1L else 0L,
        )
    }

    override suspend fun updateBookCoverUriIfEmpty(bookId: Book.Id, coverUri: String) {
        database.bookQueries.updateBookCoverUri(cover_uri = coverUri, id = bookId.value)
    }

    override suspend fun getChaptersByBook(bookId: Book.Id): List<Chapter> =
        database.chapterQueries.selectByBook(bookId.value) {
                id, bookIdVal, title, fileUri, duration, sortOrder, sourceSize, sourceLastModified ->
            Chapter(
                id = Chapter.Id(id),
                bookId = Book.Id(bookIdVal),
                title = title,
                fileUri = fileUri,
                duration = duration,
                sortOrder = sortOrder.toInt(),
                sourceSize = sourceSize,
                sourceLastModified = sourceLastModified,
            )
        }.executeAsList()

    override suspend fun getSpaces(): List<LibrarySpace> =
        database.librarySpaceQueries.selectAllSpaces(::mapSpace).executeAsList()

    override suspend fun getDefaultSpaceId(): LibrarySpace.Id {
        val id = database.librarySpaceQueries.selectDefaultSpaceId().executeAsOneOrNull()
        if (id != null) return LibrarySpace.Id(id)
        database.librarySpaceQueries.insertSpace(
            name = LibrarySpace.DEFAULT_NAME,
            icon = LibrarySpace.DEFAULT_ICON,
            sort_order = 0,
            is_default = 1,
        )
        return LibrarySpace.Id(database.librarySpaceQueries.lastSpaceInsertId().executeAsOne())
    }

    override suspend fun getSpace(id: LibrarySpace.Id): LibrarySpace? =
        database.librarySpaceQueries.selectSpaceById(id.value, ::mapSpace).executeAsOneOrNull()

    override suspend fun countSpaces(): Int =
        database.librarySpaceQueries.countSpaces().executeAsOne().toInt()

    override suspend fun createSpace(name: String, icon: String, sortOrder: Int): LibrarySpace.Id {
        database.librarySpaceQueries.insertSpace(name, icon, sortOrder.toLong(), is_default = 0)
        return LibrarySpace.Id(database.librarySpaceQueries.lastSpaceInsertId().executeAsOne())
    }

    override suspend fun updateSpace(id: LibrarySpace.Id, name: String, icon: String, sortOrder: Int) {
        database.librarySpaceQueries.updateSpace(name, icon, sortOrder.toLong(), id.value)
    }

    override suspend fun deleteSpace(id: LibrarySpace.Id) {
        database.transaction {
            database.audiobookPlaybackQueries.deletePositionsForSpace(id.value)
            database.audiobookPlaybackQueries.deleteRecentForSpace(id.value)
            database.audiobookPlaybackQueries.deleteLastPlayedForSpace(id.value)
            database.librarySpaceQueries.deleteProgressForSpace(id.value)
            database.librarySpaceQueries.deleteMembershipsForSpace(id.value)
            database.librarySpaceQueries.deleteSpace(id.value)
        }
    }

    override suspend fun addBookToSpace(spaceId: LibrarySpace.Id, bookId: Book.Id) {
        database.librarySpaceQueries.insertBookInSpace(spaceId.value, bookId.value)
    }

    override suspend fun addBooksToSpace(spaceId: LibrarySpace.Id, bookIds: List<Book.Id>) {
        database.transaction {
            bookIds.forEach { database.librarySpaceQueries.insertBookInSpace(spaceId.value, it.value) }
        }
    }

    override suspend fun addBooksBySubPathToSpace(spaceId: LibrarySpace.Id, subPath: String) {
        val ids = database.bookQueries.selectIdsBySubPath(subPath).executeAsList()
        database.transaction {
            ids.forEach { database.librarySpaceQueries.insertBookInSpace(spaceId.value, it) }
        }
    }

    override suspend fun removeBookFromSpace(spaceId: LibrarySpace.Id, bookId: Book.Id) {
        database.transaction {
            database.librarySpaceQueries.deleteBookInSpace(spaceId.value, bookId.value)
            database.librarySpaceQueries.deleteProgressForSpaceBook(spaceId.value, bookId.value)
            database.audiobookPlaybackQueries.deleteSpacePosition(spaceId.value, bookId.value)
            database.audiobookPlaybackQueries.deleteRecentForSpaceBook(spaceId.value, bookId.value)
        }
    }

    private companion object {
        fun mapBookWithProgress(
            id: Long,
            title: String,
            folderUri: String,
            chapterCount: Long,
            subPath: String,
            totalDurationMs: Long,
            listenedDurationMs: Long,
            isCompleted: Long,
            coverUri: String?,
            hiddenFromContinue: Long,
        ): Book = Book(
            id = Book.Id(id),
            title = title,
            folderUri = folderUri,
            chapterCount = chapterCount.toInt(),
            subPath = subPath,
            totalDurationMs = totalDurationMs,
            listenedDurationMs = listenedDurationMs,
            isCompleted = isCompleted != 0L,
            coverUri = coverUri,
            hiddenFromContinue = hiddenFromContinue != 0L,
        )

        fun mapSpace(
            id: Long,
            name: String,
            icon: String,
            sortOrder: Long,
            isDefault: Long,
        ): LibrarySpace = LibrarySpace(
            id = LibrarySpace.Id(id),
            name = name,
            icon = icon,
            sortOrder = sortOrder.toInt(),
            isDefault = isDefault != 0L,
        )

        fun chapterRowMatches(row: ChapterRow, scanned: ScannedChapter): Boolean =
            row.title == scanned.title &&
                row.duration == scanned.duration &&
                row.sort_order == scanned.sortOrder.toLong() &&
                row.source_size == scanned.sourceSize &&
                row.source_last_modified == scanned.sourceLastModified
    }
}
