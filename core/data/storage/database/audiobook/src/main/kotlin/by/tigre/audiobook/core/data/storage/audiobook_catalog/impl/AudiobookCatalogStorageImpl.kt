package by.tigre.audiobook.core.data.storage.audiobook_catalog.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import by.tigre.audiobook.core.data.storage.audiobook.DatabaseAudiobook
import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage
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

    override val books: Flow<List<Book>> = database.bookQueries.selectAll { id, title, folderUri, chapterCount ->
        Book(
            id = Book.Id(id),
            title = title,
            folderUri = folderUri,
            chapterCount = chapterCount.toInt()
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

    override suspend fun insertBook(title: String, folderUri: String, folderSourceId: FolderSource.Id): Book.Id {
        database.bookQueries.insertBook(title, folderUri, folderSourceId.value)
        val id = database.bookQueries.lastInsertId().executeAsOne()
        return Book.Id(id)
    }

    override suspend fun insertChapter(
        bookId: Book.Id,
        title: String,
        fileUri: String,
        duration: Long,
        sortOrder: Int
    ) {
        database.chapterQueries.insertChapter(bookId.value, title, fileUri, duration, sortOrder.toLong())
    }

    override suspend fun getBooks(): List<Book> {
        return database.bookQueries.selectAll { id, title, folderUri, chapterCount ->
            Book(
                id = Book.Id(id),
                title = title,
                folderUri = folderUri,
                chapterCount = chapterCount.toInt()
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

    override suspend fun deleteBooksByFolderSource(folderSourceId: FolderSource.Id) {
        database.bookQueries.deleteByFolderSource(folderSourceId.value)
    }
}
