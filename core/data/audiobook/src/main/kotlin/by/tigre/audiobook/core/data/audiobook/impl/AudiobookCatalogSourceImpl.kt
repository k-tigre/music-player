package by.tigre.audiobook.core.data.audiobook.impl

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage
import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage.ScannedBook
import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage.ScannedChapter
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

    override suspend fun rescanAllFolders() = withContext(Dispatchers.IO) {
        val folders = storage.getFolderSources()
        for (folder in folders) {
            scanFolder(folder.id, folder.uri)
        }
    }

    override suspend fun getBooks(): List<Book> = storage.getBooks()

    override suspend fun getChapters(bookId: Book.Id): List<Chapter> = storage.getChaptersByBook(bookId)

    private suspend fun scanFolder(folderSourceId: FolderSource.Id, uri: String) = withContext(Dispatchers.IO) {
        try {
            val treeUri = Uri.parse(uri)
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext

            val scannedBooks = mutableListOf<ScannedBook>()
            scanDirectory(rootDoc, parentPath = "", scannedBooks)
            Log.d(TAG) { "Start Scan folder $uri: ${scannedBooks.size} books found" }
            storage.syncBooksForFolder(folderSourceId, scannedBooks)
            Log.d(TAG) { "Scan complete for folder $uri: ${scannedBooks.size} books found" }
        } catch (e: Exception) {
            Log.e(e) { "Error scanning folder: $uri" }
        }
    }

    private fun scanDirectory(
        dir: DocumentFile,
        parentPath: String,
        result: MutableList<ScannedBook>
    ) {
        Log.d(TAG) { "Start scanDirectory $dir: ${parentPath}" }
        val children = dir.listFiles()
        val audioFiles = children.filter { it.isFile && isAudioFile(it) }
        val subdirs = children.filter { it.isDirectory }

        if (audioFiles.isNotEmpty()) {
            val chapters = buildChapters(audioFiles)
            val totalDuration = chapters.sumOf { it.duration }

            result.add(
                ScannedBook(
                    title = dir.name ?: "Unknown",
                    folderUri = dir.uri.toString(),
                    subPath = parentPath,
                    totalDurationMs = totalDuration,
                    chapters = chapters
                )
            )
        }

        val currentPath = if (parentPath.isEmpty()) (dir.name ?: "") else "$parentPath/${dir.name ?: ""}"
        for (subdir in subdirs) {
            scanDirectory(subdir, currentPath, result)
        }
        Log.d(TAG) { "END scanDirectory $dir: ${parentPath}" }
    }

    private fun buildChapters(audioFiles: List<DocumentFile>): List<ScannedChapter> {
        val sorted = audioFiles.sortedBy { it.name ?: "" }
        return sorted.mapIndexed { index, file ->
            val chapterTitle = file.name?.substringBeforeLast('.') ?: "Chapter ${index + 1}"
            val duration = getAudioDuration(file.uri)

            ScannedChapter(
                title = chapterTitle,
                fileUri = file.uri.toString(),
                duration = duration,
                sortOrder = index
            )
        }
    }

    private fun getAudioDuration(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e(e) { "Failed to get duration for: $uri" }
            0L
        } finally {
            retriever.release()
        }
    }

    private fun isAudioFile(file: DocumentFile): Boolean {
        val mimeType = file.type
        if (mimeType != null && mimeType.startsWith("audio/")) return true

        val name = file.name?.lowercase() ?: return false
        return AUDIO_EXTENSIONS.any { name.endsWith(it) }
    }

    private companion object {
        const val TAG = "AudiobookCatalogSource"
        val AUDIO_EXTENSIONS = listOf(".mp3", ".m4a", ".m4b", ".ogg", ".opus", ".flac")
    }
}
