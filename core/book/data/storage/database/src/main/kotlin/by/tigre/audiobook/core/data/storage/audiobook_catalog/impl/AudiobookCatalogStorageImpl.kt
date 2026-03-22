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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import audiobook.Chapter as ChapterRow

class AudiobookCatalogStorageImpl(
    private val database: DatabaseAudiobook,
    scope: CoroutineScope
) : AudiobookCatalogStorage {

    override val books: Flow<List<Book>> =
        database.bookQueries.selectAll { id, title, folderUri, chapterCount, subPath, totalDurationMs, listenedDurationMs, isCompleted ->
            Book(
                id = Book.Id(id),
                title = title,
                folderUri = folderUri,
                chapterCount = chapterCount.toInt(),
                subPath = subPath,
                totalDurationMs = totalDurationMs,
                listenedDurationMs = listenedDurationMs,
                isCompleted = isCompleted != 0L
            )
        }.asFlow().mapToList(scope.coroutineContext)
            .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    override val folderSources: Flow<List<FolderSource>> =
        database.folderSourceQueries.selectAll { id, uri, name ->
            FolderSource(
                id = FolderSource.Id(id),
                uri = uri,
                name = name
            )
        }.asFlow().mapToList(scope.coroutineContext)
            .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    override suspend fun addFolderSource(uri: String, name: String): FolderSource.Id {
        database.folderSourceQueries.insertFolder(uri, name)
        val id = database.folderSourceQueries.lastInsertId().executeAsOne()
        return FolderSource.Id(id)
    }

    override suspend fun getFolderSources(): List<FolderSource> {
        return database.folderSourceQueries.selectAll { id, uri, name ->
            FolderSource(id = FolderSource.Id(id), uri = uri, name = name)
        }.executeAsList()
    }

    override suspend fun removeFolderSource(id: FolderSource.Id) {
        database.transactionWithResult {
            database.bookQueries.deleteByFolderSource(id.value)
            database.folderSourceQueries.deleteById(id.value)
        }
    }

    override suspend fun getFolderSourceByUri(uri: String): FolderSource? {
        return database.folderSourceQueries.selectByUri(uri) { id, u, name ->
            FolderSource(id = FolderSource.Id(id), uri = u, name = name)
        }.executeAsOneOrNull()
    }

    override suspend fun syncBooksForFolder(folderSourceId: FolderSource.Id, scannedBooks: List<ScannedBook>) {
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
                        id = bookId
                    )
                } else {
                    database.bookQueries.insertBook(
                        scanned.title,
                        scanned.folderUri,
                        folderSourceId.value,
                        scanned.subPath,
                        scanned.totalDurationMs
                    )
                    bookId = database.bookQueries.lastInsertId().executeAsOne()
                }

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

    override suspend fun countBooksByFolderSource(folderSourceId: FolderSource.Id): Int {
        return database.bookQueries
            .selectBooksByFolderSourceId(folderSourceId.value)
            .executeAsList()
            .size
    }

    override suspend fun getExistingBookIdForScan(
        folderSourceId: FolderSource.Id,
        folderUri: String,
        title: String,
    ): Book.Id? {
        return database.bookQueries
            .selectIdByFolderSourceFolderUriTitle(
                folder_source_id = folderSourceId.value,
                folder_uri = folderUri,
                title = title,
            )
            .executeAsOneOrNull()
            ?.let { Book.Id(it) }
    }

    override suspend fun getBook(bookId: Book.Id): Book? {
        return database.bookQueries.selectById(bookId.value) { id, title, folderUri, chapterCount, subPath, totalDurationMs, listenedDurationMs, isCompleted ->
            Book(
                id = Book.Id(id),
                title = title,
                folderUri = folderUri,
                chapterCount = chapterCount.toInt(),
                subPath = subPath,
                totalDurationMs = totalDurationMs,
                listenedDurationMs = listenedDurationMs,
                isCompleted = isCompleted != 0L
            )
        }.executeAsOneOrNull()
    }

    override suspend fun getBooks(): List<Book> {
        return database.bookQueries.selectAll { id, title, folderUri, chapterCount, subPath, _, _, _ ->
            Book(
                id = Book.Id(id),
                title = title,
                folderUri = folderUri,
                chapterCount = chapterCount.toInt(),
                subPath = subPath
            )
        }.executeAsList()
    }

    override suspend fun getChaptersByBook(bookId: Book.Id): List<Chapter> {
        return database.chapterQueries.selectByBook(bookId.value) { id, bookIdVal, title, fileUri, duration, sortOrder, sourceSize, sourceLastModified ->
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
    }

    private companion object {
        fun chapterRowMatches(row: ChapterRow, scanned: ScannedChapter): Boolean {
            return row.title == scanned.title &&
                row.duration == scanned.duration &&
                row.sort_order == scanned.sortOrder.toLong() &&
                row.source_size == scanned.sourceSize &&
                row.source_last_modified == scanned.sourceLastModified
        }
    }
}
