package by.tigre.audiobook.core.data.audiobook.impl

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.audiobook.core.entity.catalog.FolderSource
import by.tigre.music.player.logger.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AudiobookCatalogSourceImpl(
    private val context: Context,
    private val storage: AudiobookCatalogStorage
) : AudiobookCatalogSource {

    override val books: Flow<List<Book>> = storage.books
    override val folderSources: Flow<List<FolderSource>> = storage.folderSources

    override suspend fun addFolderAndScan(uri: String, name: String) = withContext(Dispatchers.IO) {
        val existingSource = storage.getFolderSourceByUri(uri)
        val folderSourceId = existingSource?.id ?: storage.addFolderSource(uri, name)
        scanFolder(folderSourceId, uri)
    }

    override suspend fun removeFolder(id: FolderSource.Id) {
        storage.removeFolderSource(id)
    }

    override suspend fun getBooks(): List<Book> = storage.getBooks()

    override suspend fun getChapters(bookId: Book.Id): List<Chapter> = storage.getChaptersByBook(bookId)

    private suspend fun scanFolder(folderSourceId: FolderSource.Id, uri: String) = withContext(Dispatchers.IO) {
        try {
            storage.deleteBooksByFolderSource(folderSourceId)
            val treeUri = Uri.parse(uri)
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext
            scanDirectory(folderSourceId, rootDoc, parentPath = "")
        } catch (e: Exception) {
            Log.e(e) { "Error scanning folder: $uri" }
        }
    }

    private suspend fun scanDirectory(
        folderSourceId: FolderSource.Id,
        dir: DocumentFile,
        parentPath: String
    ) {
        val children = dir.listFiles()
        val audioFiles = children.filter { it.isFile && isAudioFile(it) }
        val subdirs = children.filter { it.isDirectory }

        if (audioFiles.isNotEmpty()) {
            val bookId = storage.insertBook(
                title = dir.name ?: "Unknown",
                folderUri = dir.uri.toString(),
                folderSourceId = folderSourceId,
                subPath = parentPath
            )
            insertChapters(bookId, audioFiles)
        }

        val currentPath = if (parentPath.isEmpty()) (dir.name ?: "") else "$parentPath/${dir.name ?: ""}"
        for (subdir in subdirs) {
            scanDirectory(folderSourceId, subdir, currentPath)
        }
    }

    private suspend fun insertChapters(bookId: Book.Id, audioFiles: List<DocumentFile>) {
        val sorted = audioFiles.sortedBy { it.name ?: "" }
        sorted.forEachIndexed { index, file ->
            val chapterTitle = file.name?.substringBeforeLast('.') ?: "Chapter ${index + 1}"
            storage.insertChapter(
                bookId = bookId,
                title = chapterTitle,
                fileUri = file.uri.toString(),
                duration = 0L,
                sortOrder = index
            )
        }
    }

    private fun isAudioFile(file: DocumentFile): Boolean {
        val mimeType = file.type
        if (mimeType != null && mimeType.startsWith("audio/")) return true

        val name = file.name?.lowercase() ?: return false
        return AUDIO_EXTENSIONS.any { name.endsWith(it) }
    }

    private companion object {
        val AUDIO_EXTENSIONS = listOf(".mp3", ".m4a", ".m4b", ".ogg", ".opus", ".flac")
    }
}
