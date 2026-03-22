package by.tigre.audiobook.core.data.audiobook.impl

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook.CatalogScanUi
import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage
import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage.ScannedBook
import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage.ScannedChapter
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.audiobook.core.entity.catalog.FolderSource
import by.tigre.music.player.logger.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AudiobookCatalogSourceImpl(
    private val context: Context,
    private val storage: AudiobookCatalogStorage
) : AudiobookCatalogSource {

    override val books: Flow<List<Book>> = storage.books
    override val folderSources: Flow<List<FolderSource>> = storage.folderSources

    private val _catalogScanUi = MutableStateFlow(CatalogScanUi())
    override val catalogScanUi: StateFlow<CatalogScanUi> = _catalogScanUi.asStateFlow()

    override suspend fun addFolderAndScan(uri: String, name: String) = withContext(Dispatchers.IO) {
        beginScan()
        try {
            val existingSource = storage.getFolderSourceByUri(uri)
            val folderSourceId = existingSource?.id ?: storage.addFolderSource(uri, name)
            val root = DocumentFile.fromTreeUri(context, Uri.parse(uri))
            if (root == null) {
                Log.e(TAG) { "fromTreeUri null for addFolderAndScan: $uri" }
                endScan("Cannot open folder. Grant access again.")
                return@withContext
            }
            val pending = mutableListOf<PendingBook>()
            val total = collectBooks(root, parentPath = "", out = pending)
            _catalogScanUi.value = _catalogScanUi.value.copy(
                total = total,
                processed = 0,
                detail = "Reading metadata…",
            )
            var processed = 0
            val scanned = buildScannedBooksForFolder(folderSourceId, pending) {
                processed++
                _catalogScanUi.value = _catalogScanUi.value.copy(processed = processed)
            }
            storage.syncBooksForFolder(folderSourceId, scanned)
            Log.d(TAG) { "Scan complete for folder $uri: ${scanned.size} books, $total files" }
            endScan("Updated ${scanned.size} books · $total files")
        } catch (e: Exception) {
            Log.e(e) { "Error in addFolderAndScan: $uri" }
            endScan(e.message ?: "Scan failed")
        }
    }

    override suspend fun removeFolder(id: FolderSource.Id) {
        storage.removeFolderSource(id)
    }

    override suspend fun rescanAllFolders() = withContext(Dispatchers.IO) {
        beginScan()
        try {
            val folders = storage.getFolderSources()
            if (folders.isEmpty()) {
                Log.w(TAG) { "rescanAllFolders: no folder sources" }
                endScan("No folders to scan.")
                return@withContext
            }

            _catalogScanUi.value = _catalogScanUi.value.copy(detail = "Collecting files…")
            val works = mutableListOf<FolderScanWork>()
            val failedNames = mutableListOf<String>()
            for (folder in folders) {
                val root = DocumentFile.fromTreeUri(context, Uri.parse(folder.uri))
                if (root == null) {
                    Log.e(TAG) { "fromTreeUri null for rescan: ${folder.uri} (${folder.name})" }
                    failedNames.add(folder.name)
                    continue
                }
                val pending = mutableListOf<PendingBook>()
                val fileCount = collectBooks(root, parentPath = "", out = pending)
                works.add(FolderScanWork(folder.id, pending, fileCount))
            }

            if (works.isEmpty()) {
                val hint = if (failedNames.isNotEmpty()) {
                    "Cannot open: ${failedNames.joinToString()}. Re-add folders."
                } else {
                    "Nothing to scan."
                }
                endScan(hint)
                return@withContext
            }

            val totalFiles = works.sumOf { it.fileCount }
            _catalogScanUi.value = _catalogScanUi.value.copy(
                total = totalFiles,
                processed = 0,
                detail = "Reading metadata…",
            )

            var processed = 0
            var booksCount = 0
            for (work in works) {
                val scanned = buildScannedBooksForFolder(work.folderSourceId, work.pending) {
                    processed++
                    _catalogScanUi.value = _catalogScanUi.value.copy(processed = processed)
                }
                booksCount += scanned.size
                storage.syncBooksForFolder(work.folderSourceId, scanned)
            }

            val suffix = if (failedNames.isNotEmpty()) {
                " (${failedNames.size} folder(s) inaccessible)"
            } else {
                ""
            }
            Log.d(TAG) { "rescanAllFolders done: $booksCount books, $totalFiles files$suffix" }
            endScan("Indexed $booksCount books · $totalFiles files$suffix")
        } catch (e: Exception) {
            Log.e(e) { "Error in rescanAllFolders" }
            endScan(e.message ?: "Scan failed")
        }
    }

    override suspend fun getBooks(): List<Book> = storage.getBooks()

    override suspend fun getBook(bookId: Book.Id): Book? = storage.getBook(bookId)

    override suspend fun getChapters(bookId: Book.Id): List<Chapter> = storage.getChaptersByBook(bookId)

    private fun beginScan() {
        _catalogScanUi.value = CatalogScanUi(
            active = true,
            processed = 0,
            total = 0,
            detail = "Preparing…",
            completedSummary = "",
        )
    }

    private fun endScan(summary: String) {
        _catalogScanUi.value = CatalogScanUi(
            active = false,
            completedSummary = summary,
        )
    }

    private suspend fun buildScannedBooksForFolder(
        folderSourceId: FolderSource.Id,
        pending: List<PendingBook>,
        onFileProcessed: () -> Unit,
    ): List<ScannedBook> {
        val result = ArrayList<ScannedBook>(pending.size)
        for (book in pending) {
            val bookId = storage.getExistingBookIdForScan(folderSourceId, book.folderUri, book.title)
            val existingByUri = if (bookId != null) {
                storage.getChaptersByBook(bookId).associateBy { it.fileUri }
            } else {
                emptyMap()
            }
            val chapters = buildChapters(book.audioFiles, existingByUri, onFileProcessed)
            val totalDuration = chapters.sumOf { it.duration }
            result.add(
                ScannedBook(
                    title = book.title,
                    folderUri = book.folderUri,
                    subPath = book.subPath,
                    totalDurationMs = totalDuration,
                    chapters = chapters,
                )
            )
        }
        return result
    }

    private fun buildChapters(
        audioFiles: List<DocumentFile>,
        existingByUri: Map<String, Chapter>,
        onFileProcessed: () -> Unit,
    ): List<ScannedChapter> {
        val sorted = audioFiles.sortedBy { it.name ?: "" }
        return sorted.mapIndexed { index, file ->
            val chapterTitle = file.name?.substringBeforeLast('.') ?: "Chapter ${index + 1}"
            val uriStr = file.uri.toString()
            val existing = existingByUri[uriStr]
            val duration =
                if (existing == null || shouldReadDurationFromMedia(existing, file)) {
                    getAudioDuration(file.uri)
                } else {
                    existing.duration
                }
            onFileProcessed()
            ScannedChapter(
                title = chapterTitle,
                fileUri = uriStr,
                duration = duration,
                sortOrder = index,
                sourceSize = file.length(),
                sourceLastModified = file.lastModified(),
            )
        }
    }

    private fun shouldReadDurationFromMedia(existing: Chapter, file: DocumentFile): Boolean {
        val storedSize = existing.sourceSize
        val storedMod = existing.sourceLastModified
        if (storedSize == null || storedMod == null) return true
        return storedSize != file.length() || storedMod != file.lastModified()
    }

    private fun collectBooks(dir: DocumentFile, parentPath: String, out: MutableList<PendingBook>): Int {
        val children = (dir.listFiles() ?: emptyArray()).filterNotNull()
        val audioFiles = children.filter { it.isFile && isAudioFile(it) }
        val subdirs = children.filter { it.isDirectory }

        var fileCount = 0
        if (audioFiles.isNotEmpty()) {
            out.add(
                PendingBook(
                    title = dir.name ?: "Unknown",
                    folderUri = dir.uri.toString(),
                    subPath = parentPath,
                    audioFiles = audioFiles,
                )
            )
            fileCount += audioFiles.size
        }

        val currentPath = if (parentPath.isEmpty()) (dir.name ?: "") else "$parentPath/${dir.name ?: ""}"
        for (subdir in subdirs) {
            fileCount += collectBooks(subdir, currentPath, out)
        }
        return fileCount
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

    private data class PendingBook(
        val title: String,
        val folderUri: String,
        val subPath: String,
        val audioFiles: List<DocumentFile>,
    )

    private data class FolderScanWork(
        val folderSourceId: FolderSource.Id,
        val pending: List<PendingBook>,
        val fileCount: Int,
    )

    private companion object {
        const val TAG = "AudiobookCatalogSource"
        val AUDIO_EXTENSIONS = listOf(".mp3", ".m4a", ".m4b", ".ogg", ".opus", ".flac")
    }
}
