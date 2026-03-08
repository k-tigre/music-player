package by.tigre.audiobook.core.data.storage.audiobook_catalog.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import by.tigre.audiobook.core.data.storage.audiobook.DatabaseAudiobook
import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage
import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage.ScannedBook
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.audiobook.core.entity.catalog.FolderSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

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

            // Delete books that no longer exist in the scanned folder
            for (existing in existingBooks) {
                val key = existing.folder_uri to existing.title
                if (key !in scannedByKey) {
                    database.bookQueries.deleteById(existing.id)
                }
            }

            // Insert or update books
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
                    database.chapterQueries.deleteByBook(bookId)
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

                for (chapter in scanned.chapters) {
                    database.chapterQueries.insertChapter(
                        bookId,
                        chapter.title,
                        chapter.fileUri,
                        chapter.duration,
                        chapter.sortOrder.toLong()
                    )
                }
            }
        }
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
        return database.chapterQueries.selectByBook(bookId.value) { id, bookIdVal, title, fileUri, duration, sortOrder ->
            Chapter(
                id = Chapter.Id(id),
                bookId = Book.Id(bookIdVal),
                title = title,
                fileUri = fileUri,
                duration = duration,
                sortOrder = sortOrder.toInt()
            )
        }.executeAsList()
    }
}
