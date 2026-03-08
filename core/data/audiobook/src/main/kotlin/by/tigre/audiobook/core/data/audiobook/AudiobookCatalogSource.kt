package by.tigre.audiobook.core.data.audiobook

import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.audiobook.core.entity.catalog.FolderSource
import kotlinx.coroutines.flow.Flow

interface AudiobookCatalogSource {
    val books: Flow<List<Book>>
    val folderSources: Flow<List<FolderSource>>

    suspend fun addFolderAndScan(uri: String, name: String)
    suspend fun removeFolder(id: FolderSource.Id)
    suspend fun rescanAllFolders()
    suspend fun getBooks(): List<Book>
    suspend fun getBook(bookId: Book.Id): Book?
    suspend fun getChapters(bookId: Book.Id): List<Chapter>
}
