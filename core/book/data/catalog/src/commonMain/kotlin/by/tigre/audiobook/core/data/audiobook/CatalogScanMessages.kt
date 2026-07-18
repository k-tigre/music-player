package by.tigre.audiobook.core.data.audiobook

enum class CatalogScanDetail {
    Preparing,
    CollectingFiles,
    ReadingMetadata,
}

sealed class CatalogScanSummary {
    data object CannotOpenFolder : CatalogScanSummary()
    data object CannotReadFolder : CatalogScanSummary()
    data object NoFilesSeenAccessIssue : CatalogScanSummary()
    data class UpdatedBooks(val books: Int, val files: Int) : CatalogScanSummary()
    data object ScanFailed : CatalogScanSummary()
    data object Cancelled : CatalogScanSummary()
    data object NoFoldersToScan : CatalogScanSummary()
    data class CannotOpenFolders(val names: List<String>) : CatalogScanSummary()
    data object NothingToScan : CatalogScanSummary()
    data class NoFilesReadAccess(val folderNames: List<String>) : CatalogScanSummary()
    data object NothingIndexed : CatalogScanSummary()
    data class Indexed(
        val books: Int,
        val files: Int,
        val problemFolders: List<String> = emptyList(),
    ) : CatalogScanSummary()
}
