package by.tigre.audiobook.core.data.audiobook

data class CatalogScanUi(
    val active: Boolean = false,
    val processed: Int = 0,
    val total: Int = 0,
    val detail: String = "",
    val completedSummary: String = "",
)
