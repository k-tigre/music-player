package by.tigre.audiobook.core.presentation.audiobook_catalog.scan

import by.tigre.audiobook.core.data.audiobook.CatalogScanUi
import kotlinx.coroutines.flow.StateFlow

interface CatalogScanCoordinator {
    val catalogScanUi: StateFlow<CatalogScanUi>

    fun addFolderAndScan(uri: String, name: String)
    fun rescanAllFolders()
    fun cancelScan()
}
