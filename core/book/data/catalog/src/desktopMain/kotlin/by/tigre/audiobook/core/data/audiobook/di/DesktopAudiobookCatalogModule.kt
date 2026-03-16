package by.tigre.audiobook.core.data.audiobook.di

import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.audiobook.core.entity.catalog.FolderSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DesktopAudiobookCatalogModule : AudiobookCatalogModule {
    override val audiobookCatalogSource: AudiobookCatalogSource by lazy {
        object : AudiobookCatalogSource {
            override val books: Flow<List<Book>> = flowOf(emptyList())
            override val folderSources: Flow<List<FolderSource>> = flowOf(emptyList())
            override suspend fun addFolderAndScan(uri: String, name: String) = Unit
            override suspend fun removeFolder(id: FolderSource.Id) = Unit
            override suspend fun rescanAllFolders() = Unit
            override suspend fun getBooks(): List<Book> = emptyList()
            override suspend fun getBook(bookId: Book.Id): Book? = null
            override suspend fun getChapters(bookId: Book.Id): List<Chapter> = emptyList()
        }
    }
}
