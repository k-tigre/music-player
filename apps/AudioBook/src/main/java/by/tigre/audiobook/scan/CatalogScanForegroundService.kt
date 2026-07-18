package by.tigre.audiobook.scan

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import by.tigre.audiobook.App
import by.tigre.audiobook.MainActivity
import by.tigre.audiobook.R
import by.tigre.audiobook.core.data.audiobook.CatalogScanDetail
import by.tigre.audiobook.core.data.audiobook.CatalogScanSummary
import by.tigre.audiobook.core.data.audiobook.CatalogScanUi
import by.tigre.logger.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CatalogScanForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collectJob: Job? = null
    private var finishing = false
    private var seenActive = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG) { "onCreate" }
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG) {
            "onStartCommand startId=$startId flags=$flags collectJobNull=${collectJob == null} " +
                "seenActive=$seenActive finishing=$finishing"
        }
        if (collectJob == null) {
            startAsForeground(buildProgressNotification(CatalogScanUi(active = true)))
            collectJob = serviceScope.launch {
                val scanUi = (application as App).graph.catalogScanCoordinator.catalogScanUi
                scanUi.collect { ui ->
                    when {
                        ui.active -> {
                            if (!seenActive) {
                                Log.i(TAG) { "scan became active processed=${ui.processed} total=${ui.total}" }
                            }
                            seenActive = true
                            finishing = false
                            startAsForeground(buildProgressNotification(ui))
                        }
                        seenActive && !finishing -> {
                            finishing = true
                            Log.i(TAG) {
                                "scan became inactive summary=${ui.completedSummary} → finish service"
                            }
                            handleScanFinished(ui.completedSummary)
                        }
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun handleScanFinished(summary: CatalogScanSummary?) {
        Log.i(TAG) { "handleScanFinished summary=$summary" }
        when (summary) {
            null, CatalogScanSummary.Cancelled -> {
                Log.i(TAG) { "stop without Done notification (cancelled/null)" }
                stopForegroundCompat()
                stopSelf()
            }
            else -> {
                val text = formatSummary(summary)
                val done = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_scan)
                    .setContentTitle(getString(R.string.catalog_scan_notification_done_title))
                    .setContentText(text)
                    .setContentIntent(contentPendingIntent())
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setOngoing(false)
                    .build()
                stopForegroundCompat()
                notificationManager().notify(DONE_NOTIFICATION_ID, done)
                delay(DONE_DISMISS_MS)
                notificationManager().cancel(DONE_NOTIFICATION_ID)
                Log.i(TAG) { "Done notification dismissed → stopSelf" }
                stopSelf()
            }
        }
    }

    private fun startAsForeground(notification: Notification) {
        val nm = notificationManager()
        Log.i(TAG) {
            "startAsForeground notificationsEnabled=${nm.areNotificationsEnabled()} " +
                "channelImportance=${nm.getNotificationChannel(CHANNEL_ID)?.importance}"
        }
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                ServiceCompat.startForeground(
                    this,
                    PROGRESS_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                startForeground(PROGRESS_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(e) { "$TAG: startForeground failed" }
        }
    }

    private fun stopForegroundCompat() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun buildProgressNotification(ui: CatalogScanUi): Notification {
        val detail = formatDetail(ui.detail)
        val progressText = if (ui.total > 0) {
            getString(R.string.catalog_scan_notification_progress, ui.processed, ui.total)
        } else {
            detail
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_scan)
            .setContentTitle(getString(R.string.catalog_scan_notification_title))
            .setContentText(if (ui.total > 0) "$detail · $progressText" else detail)
            .setContentIntent(contentPendingIntent())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
        if (ui.total > 0) {
            builder.setProgress(ui.total, ui.processed, false)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    private fun formatDetail(detail: CatalogScanDetail): String = when (detail) {
        CatalogScanDetail.Preparing -> getString(R.string.catalog_scan_detail_preparing)
        CatalogScanDetail.CollectingFiles -> getString(R.string.catalog_scan_detail_collecting)
        CatalogScanDetail.ReadingMetadata -> getString(R.string.catalog_scan_detail_metadata)
    }

    private fun formatSummary(summary: CatalogScanSummary): String = when (summary) {
        CatalogScanSummary.CannotOpenFolder ->
            getString(R.string.catalog_scan_summary_cannot_open_folder)
        CatalogScanSummary.CannotReadFolder ->
            getString(R.string.catalog_scan_summary_cannot_read_folder)
        CatalogScanSummary.NoFilesSeenAccessIssue ->
            getString(R.string.catalog_scan_summary_no_files_seen)
        is CatalogScanSummary.UpdatedBooks ->
            getString(R.string.catalog_scan_summary_updated_books, summary.books, summary.files)
        CatalogScanSummary.ScanFailed ->
            getString(R.string.catalog_scan_summary_failed)
        CatalogScanSummary.Cancelled ->
            getString(R.string.catalog_scan_summary_cancelled)
        CatalogScanSummary.NoFoldersToScan ->
            getString(R.string.catalog_scan_summary_no_folders)
        is CatalogScanSummary.CannotOpenFolders ->
            getString(R.string.catalog_scan_summary_cannot_open_folders, summary.names.joinToString())
        CatalogScanSummary.NothingToScan ->
            getString(R.string.catalog_scan_summary_nothing)
        is CatalogScanSummary.NoFilesReadAccess ->
            getString(
                R.string.catalog_scan_summary_no_files_read,
                summary.folderNames.joinToString(),
            )
        CatalogScanSummary.NothingIndexed ->
            getString(R.string.catalog_scan_summary_nothing_indexed)
        is CatalogScanSummary.Indexed -> {
            val base = getString(
                R.string.catalog_scan_summary_indexed,
                summary.books,
                summary.files,
            )
            if (summary.problemFolders.isEmpty()) {
                base
            } else {
                base + getString(
                    R.string.catalog_scan_summary_skipped,
                    summary.problemFolders.size,
                )
            }
        }
    }

    private fun contentPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.catalog_scan_notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.catalog_scan_notification_channel_desc)
            setShowBadge(false)
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager(): NotificationManager =
        getSystemService(NotificationManager::class.java)

    override fun onDestroy() {
        Log.w(Exception("CatalogScanForegroundService.onDestroy stack"), TAG) {
            "onDestroy seenActive=$seenActive finishing=$finishing " +
                "uiActive=${runCatching { (application as App).graph.catalogScanCoordinator.catalogScanUi.value.active }.getOrNull()}"
        }
        collectJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "catalog_scan_v2"
        private const val TAG = "CatalogScan"
        private const val PROGRESS_NOTIFICATION_ID = 4101
        private const val DONE_NOTIFICATION_ID = 4102
        private const val DONE_DISMISS_MS = 7_000L
    }
}
