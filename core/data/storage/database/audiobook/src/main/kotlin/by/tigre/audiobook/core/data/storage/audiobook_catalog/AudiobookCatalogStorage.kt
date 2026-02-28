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

    suspend fun insertBook(title: String, folderUri: String, folderSourceId: FolderSource.Id): Book.Id
    suspend fun insertChapter(bookId: Book.Id, title: String, fileUri: String, duration: Long, sortOrder: Int)

    suspend fun getBooks(): List<Book>
    suspend fun getChaptersByBook(bookId: Book.Id): List<Chapter>

    suspend fun deleteBooksByFolderSource(folderSourceId: FolderSource.Id)
}
