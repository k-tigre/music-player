package by.tigre.audiobook.scan

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook.CatalogScanUi
import by.tigre.audiobook.core.presentation.audiobook_catalog.scan.CatalogScanCoordinator
import by.tigre.logger.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CatalogScanCoordinatorImpl(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val catalogSource: AudiobookCatalogSource,
) : CatalogScanCoordinator {

    private var scanJob: Job? = null

    override val catalogScanUi: StateFlow<CatalogScanUi> = catalogSource.catalogScanUi

    override fun addFolderAndScan(uri: String, name: String) {
        Log.i(TAG) { "addFolderAndScan requested uri=$uri name=$name activeJob=${scanJob?.isActive}" }
        startScan(reason = "addFolder") { catalogSource.addFolderAndScan(uri, name) }
    }

    override fun rescanAllFolders() {
        Log.i(TAG) { "rescanAllFolders requested activeJob=${scanJob?.isActive}" }
        startScan(reason = "rescan") { catalogSource.rescanAllFolders() }
    }

    override fun cancelScan() {
        val active = scanJob?.isActive == true
        // Stack traces who called cancel (banner button vs unexpected).
        Log.w(Exception("cancelScan stack"), TAG) {
            "cancelScan called activeJob=$active uiActive=${catalogScanUi.value.active}"
        }
        scanJob?.cancel(CancellationException("cancelScan() from UI/API"))
    }

    private fun startScan(reason: String, block: suspend () -> Unit) {
        if (scanJob?.isActive == true) {
            Log.w(TAG) { "startScan($reason) ignored — job already active" }
            return
        }
        startForegroundService()
        scanJob = scope.launch {
            Log.i(TAG) { "scan job started reason=$reason" }
            try {
                block()
                Log.i(TAG) {
                    "scan job finished normally reason=$reason summary=${catalogScanUi.value.completedSummary}"
                }
            } catch (e: CancellationException) {
                Log.w(TAG) { "scan job cancelled reason=$reason msg=${e.message}" }
                throw e
            } catch (e: Exception) {
                Log.e(e) { "$TAG: scan job failed reason=$reason" }
            }
        }
    }

    private fun startForegroundService() {
        Log.i(TAG) { "starting CatalogScanForegroundService" }
        val intent = Intent(appContext, CatalogScanForegroundService::class.java)
        ContextCompat.startForegroundService(appContext, intent)
    }

    private companion object {
        const val TAG = "CatalogScan"
    }
}
