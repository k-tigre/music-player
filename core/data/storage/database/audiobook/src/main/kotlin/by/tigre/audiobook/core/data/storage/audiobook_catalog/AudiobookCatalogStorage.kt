package by.tigre.audiobook.core.data.storage.audiobook_catalog

import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.audiobook.core.entity.catalog.FolderSource
import kotlinx.coroutines.flow.Flow

interface AudiobookCatalogStorage {
    val books: Flow<List<Book>>
    val folderSources: Flow<List<FolderSource>>

    suspend fun addFolderSource(uri: String, name: String): FolderSource.Id
    suspend fun getFolderSources(): List<FolderSource>
    suspend fun removeFolderSource(id: FolderSource.Id)
    suspend fun getFolderSourceByUri(uri: String): FolderSource?

    suspend fun syncBooksForFolder(folderSourceId: FolderSource.Id, scannedBooks: List<ScannedBook>)

    suspend fun getBooks(): List<Book>
    suspend fun getBook(bookId: Book.Id): Book?
    suspend fun getChaptersByBook(bookId: Book.Id): List<Chapter>

    data class ScannedBook(
        val title: String,
        val folderUri: String,
        val subPath: String,
        val totalDurationMs: Long,
        val chapters: List<ScannedChapter>
    )

    data class ScannedChapter(
        val title: String,
        val fileUri: String,
        val duration: Long,
        val sortOrder: Int
    )
}
