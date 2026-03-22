package by.tigre.audiobook.core.data.audiobook.impl

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook.CatalogScanUi
import by.tigre.audiobook.core.data.audiobook.FolderSourceAccessHealth
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
            val total = when (val collected = collectBooks(root, parentPath = "", out = pending)) {
                is CollectResult.Ok -> collected.fileCount
                CollectResult.ListingFailed -> {
                    Log.e(TAG) { "Cannot list folder contents (permission?): $uri" }
                    endScan("Cannot read folder. Open it again via + or re-add the folder.")
                    return@withContext
                }
            }
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
            val hadBooks = storage.countBooksByFolderSource(folderSourceId) > 0
            if (scanned.isEmpty() && hadBooks) {
                Log.w(TAG) { "addFolderAndScan: 0 books found but DB already has books for source $folderSourceId — skip sync" }
                endScan("No files seen (access issue?). Your catalog was not changed. Re-pick the folder if needed.")
                return@withContext
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
                when (val collected = collectBooks(root, parentPath = "", out = pending)) {
                    is CollectResult.Ok ->
                        works.add(FolderScanWork(folder.id, folder.name, pending, collected.fileCount))
                    CollectResult.ListingFailed -> {
                        Log.e(TAG) { "listFiles failed for rescan: ${folder.uri} (${folder.name})" }
                        failedNames.add(folder.name)
                    }
                }
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
                val hadBooks = storage.countBooksByFolderSource(work.folderSourceId) > 0
                if (scanned.isEmpty() && hadBooks) {
                    Log.w(TAG) {
                        "rescan: 0 books for ${work.folderName} (source ${work.folderSourceId}) but DB has entries — skip sync"
                    }
                    failedNames.add(work.folderName)
                    continue
                }
                booksCount += scanned.size
                storage.syncBooksForFolder(work.folderSourceId, scanned)
            }

            val problemFolders = failedNames.distinct()
            val suffix = when {
                problemFolders.isEmpty() -> ""
                problemFolders.size <= 2 ->
                    " — skipped or failed: ${problemFolders.joinToString()}"

                else ->
                    " — skipped or failed: ${problemFolders.size} folders"
            }
            Log.d(TAG) { "rescanAllFolders done: $booksCount books, $totalFiles files$suffix" }
            val summary = when {
                booksCount == 0 && totalFiles == 0 && problemFolders.isNotEmpty() ->
                    "No files were read (storage access). Catalog left unchanged. Re-add or re-pick: ${
                        problemFolders.take(2).joinToString()
                    }"

                booksCount == 0 && totalFiles == 0 ->
                    "Nothing indexed. Check folder access or file types."

                else ->
                    "Indexed $booksCount books · $totalFiles files$suffix"
            }
            endScan(summary)
        } catch (e: Exception) {
            Log.e(e) { "Error in rescanAllFolders" }
            endScan(e.message ?: "Scan failed")
        }
    }

    override suspend fun getBooks(): List<Book> = storage.getBooks()

    override suspend fun getBook(bookId: Book.Id): Book? = storage.getBook(bookId)

    override suspend fun getChapters(bookId: Book.Id): List<Chapter> = storage.getChaptersByBook(bookId)

    override suspend fun getFolderSourcesList(): List<FolderSource> = storage.getFolderSources()

    override suspend fun diagnoseFolderAccess(folder: FolderSource): FolderSourceAccessHealth =
        withContext(Dispatchers.IO) {
            val root = DocumentFile.fromTreeUri(context, Uri.parse(folder.uri))
                ?: return@withContext FolderSourceAccessHealth.TreeUriUnavailable

            @Suppress("UNCHECKED_CAST")
            val raw = root.listFiles() as? Array<out DocumentFile>?
                ?: return@withContext FolderSourceAccessHealth.CannotListContents

            if (raw.isEmpty()) {
                val hadBooks = storage.countBooksByFolderSource(folder.id) > 0
                return@withContext if (hadBooks) {
                    FolderSourceAccessHealth.ListedButEmptyWithIndexedBooks
                } else {
                    FolderSourceAccessHealth.Ok
                }
            }
            FolderSourceAccessHealth.Ok
        }

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
            val coverUri = resolveCoverUri(book.bookDir, book.title)
            result.add(
                ScannedBook(
                    title = book.title,
                    folderUri = book.folderUri,
                    subPath = book.subPath,
                    totalDurationMs = totalDuration,
                    coverUri = coverUri,
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

    /**
     * [CollectResult.ListingFailed]: [DocumentFile.listFiles] returned null — do not treat as an empty library
     * (that would sync an empty list and delete all books). Usually lost SAF access.
     */
    private sealed class CollectResult {
        data class Ok(val fileCount: Int) : CollectResult()
        data object ListingFailed : CollectResult()
    }

    private fun collectBooks(dir: DocumentFile, parentPath: String, out: MutableList<PendingBook>): CollectResult {
        val frameStart = out.size
        @Suppress("UNCHECKED_CAST") // Java API may return null; stubs are non-null
        val raw = dir.listFiles() as Array<out DocumentFile>?
        if (raw == null) {
            Log.w(TAG) { "listFiles() returned null at ${dir.uri}" }
            return CollectResult.ListingFailed
        }
        val children = raw.asList()
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
                    bookDir = dir,
                )
            )
            fileCount += audioFiles.size
        }

        val currentPath = if (parentPath.isEmpty()) (dir.name ?: "") else "$parentPath/${dir.name ?: ""}"
        for (subdir in subdirs) {
            when (val sub = collectBooks(subdir, currentPath, out)) {
                CollectResult.ListingFailed -> {
                    while (out.size > frameStart) {
                        out.removeAt(out.size - 1)
                    }
                    return CollectResult.ListingFailed
                }
                is CollectResult.Ok -> fileCount += sub.fileCount
            }
        }
        return CollectResult.Ok(fileCount)
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

    private fun isImageFile(file: DocumentFile): Boolean {
        val mimeType = file.type
        if (mimeType != null && mimeType.startsWith("image/")) return true
        val name = file.name?.lowercase() ?: return false
        return IMAGE_EXTENSIONS.any { name.endsWith(it) }
    }

    private fun listImageFiles(dir: DocumentFile): List<DocumentFile> {
        @Suppress("UNCHECKED_CAST")
        val raw = dir.listFiles() as Array<out DocumentFile>? ?: return emptyList()
        return raw.filter { it.isFile && isImageFile(it) }.sortedBy { it.name ?: "" }
    }

    /**
     * Picks a cover from the book folder (same dir as chapters): common filenames first,
     * then name closest to the book title, otherwise the first image by name.
     * If the folder has no images, uses the parent folder (series-wide cover).
     */
    private fun resolveCoverUri(bookDir: DocumentFile, bookTitle: String): String? {
        val local = listImageFiles(bookDir)
        val pick = if (local.isNotEmpty()) {
            pickBestCoverImage(local, bookTitle)
        } else {
            val parent = bookDir.parentFile ?: return null
            pickBestCoverImage(listImageFiles(parent), bookTitle)
        }
        return pick?.uri?.toString()
    }

    private fun pickBestCoverImage(files: List<DocumentFile>, bookTitle: String): DocumentFile? {
        if (files.isEmpty()) return null
        val nTitle = normalizeForMatch(bookTitle)
        var bestFile = files.first()
        var bestScore = Int.MIN_VALUE
        for (file in files) {
            val stem = normalizeForMatch(file.name?.substringBeforeLast('.') ?: "")
            val score = scoreCoverStem(stem, nTitle)
            if (score > bestScore) {
                bestScore = score
                bestFile = file
            }
        }
        return if (bestScore >= FUZZY_COVER_MIN_SCORE) bestFile else files.first()
    }

    private fun scoreCoverStem(stem: String, nTitle: String): Int {
        if (stem.isEmpty()) return 0
        if (stem in COMMON_COVER_STEMS) return 900
        if (nTitle.isNotEmpty() && stem == nTitle) return 800
        if (nTitle.length >= 3 && (stem.contains(nTitle) || nTitle.contains(stem))) {
            return 500 + minOf(stem.length, nTitle.length)
        }
        val maxLen = maxOf(stem.length, nTitle.length)
        if (maxLen == 0 || nTitle.isEmpty()) return 0
        val dist = levenshtein(stem, nTitle)
        return ((maxLen - dist).coerceAtLeast(0) * 350) / maxLen
    }

    private fun normalizeForMatch(s: String): String = buildString(s.length) {
        for (c in s.lowercase()) {
            when {
                c.isLetterOrDigit() -> append(c)
            }
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val prev = IntArray(b.length + 1) { it }
        val cur = IntArray(b.length + 1)
        for (i in a.indices) {
            cur[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                cur[j + 1] = minOf(
                    cur[j] + 1,
                    prev[j + 1] + 1,
                    prev[j] + cost,
                )
            }
            prev.indices.forEach { prev[it] = cur[it] }
        }
        return prev[b.length]
    }

    private data class PendingBook(
        val title: String,
        val folderUri: String,
        val subPath: String,
        val audioFiles: List<DocumentFile>,
        val bookDir: DocumentFile,
    )

    private data class FolderScanWork(
        val folderSourceId: FolderSource.Id,
        val folderName: String,
        val pending: List<PendingBook>,
        val fileCount: Int,
    )

    private companion object {
        const val TAG = "AudiobookCatalogSource"
        val AUDIO_EXTENSIONS = listOf(".mp3", ".m4a", ".m4b", ".ogg", ".opus", ".flac")
        val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".heic", ".heif")
        val COMMON_COVER_STEMS = setOf(
            "cover", "folder", "front", "album", "poster", "art", "book",
            "обложка", "coverart",
        )

        /** Below this, treat as "no good title match" and use first image in lexicographic order. */
        const val FUZZY_COVER_MIN_SCORE = 180
    }
}
