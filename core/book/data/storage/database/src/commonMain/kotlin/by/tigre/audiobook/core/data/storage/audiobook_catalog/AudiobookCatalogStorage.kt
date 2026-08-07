package by.tigre.audiobook.core.data.storage.audiobook_catalog

import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.audiobook.core.entity.catalog.FolderSource
import by.tigre.audiobook.core.entity.catalog.LibrarySpace
import kotlinx.coroutines.flow.Flow

interface AudiobookCatalogStorage {
    fun observeBooksInSpace(spaceId: LibrarySpace.Id): Flow<List<Book>>
    fun observeContinueListeningInSpace(spaceId: LibrarySpace.Id): Flow<List<Book>>
    val folderSources: Flow<List<FolderSource>>

    suspend fun addFolderSource(uri: String, name: String): FolderSource.Id
    suspend fun getFolderSources(): List<FolderSource>
    suspend fun removeFolderSource(id: FolderSource.Id)
    suspend fun getFolderSourceByUri(uri: String): FolderSource?

    suspend fun syncBooksForFolder(folderSourceId: FolderSource.Id, scannedBooks: List<ScannedBook>)

    suspend fun countBooksByFolderSource(folderSourceId: FolderSource.Id): Int

    suspend fun getExistingBookIdForScan(
        folderSourceId: FolderSource.Id,
        folderUri: String,
        title: String,
    ): Book.Id?

    suspend fun getBooksGlobal(): List<Book>
    suspend fun getBooksInSpace(spaceId: LibrarySpace.Id): List<Book>
    suspend fun getBookInSpace(spaceId: LibrarySpace.Id, bookId: Book.Id): Book?
    suspend fun getBookGlobal(bookId: Book.Id): Book?
    suspend fun getChaptersByBook(bookId: Book.Id): List<Chapter>
    suspend fun setHiddenFromContinue(spaceId: LibrarySpace.Id, bookId: Book.Id, hidden: Boolean)
    suspend fun updateBookCoverUriIfEmpty(bookId: Book.Id, coverUri: String)

    suspend fun getSpaces(): List<LibrarySpace>
    suspend fun getDefaultSpaceId(): LibrarySpace.Id
    suspend fun getSpace(id: LibrarySpace.Id): LibrarySpace?
    suspend fun countSpaces(): Int
    suspend fun createSpace(name: String, icon: String, sortOrder: Int): LibrarySpace.Id
    suspend fun updateSpace(id: LibrarySpace.Id, name: String, icon: String, sortOrder: Int)
    suspend fun deleteSpace(id: LibrarySpace.Id)
    suspend fun addBookToSpace(spaceId: LibrarySpace.Id, bookId: Book.Id)
    suspend fun addBooksToSpace(spaceId: LibrarySpace.Id, bookIds: List<Book.Id>)
    suspend fun addBooksBySubPathToSpace(spaceId: LibrarySpace.Id, subPath: String)
    suspend fun removeBookFromSpace(spaceId: LibrarySpace.Id, bookId: Book.Id)

    data class ScannedBook(
        val title: String,
        val folderUri: String,
        val subPath: String,
        val totalDurationMs: Long,
        val coverUri: String?,
        val chapters: List<ScannedChapter>
    )

    data class ScannedChapter(
        val title: String,
        val fileUri: String,
        val duration: Long,
        val sortOrder: Int,
        val sourceSize: Long?,
        val sourceLastModified: Long?,
    )
}
