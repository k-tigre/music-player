package by.tigre.audiobook.core.presentation.audiobook_catalog.view

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import by.tigre.audiobook.core.data.audiobook.CatalogScanDetail
import by.tigre.audiobook.core.data.audiobook.CatalogScanSummary
import by.tigre.audiobook.core.data.audiobook.CatalogScanUi
import `by`.tigre.audiobook.core.presentation.catalog.resources.Res
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_banner_cancel
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_detail_collecting_files
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_detail_preparing
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_detail_reading_metadata
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_files_progress
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_summary_cancelled
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_summary_cannot_open_folder
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_summary_cannot_open_folders
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_summary_cannot_read_folder
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_summary_failed
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_summary_indexed
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_summary_no_files_read_access
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_summary_no_files_seen
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_summary_no_folders_to_scan
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_summary_nothing_indexed
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_summary_nothing_to_scan
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_summary_skipped_or_failed_count
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_summary_skipped_or_failed_names
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_summary_updated_books
import `by`.tigre.audiobook.core.presentation.catalog.resources.scanning_folders_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun CatalogScanProgressBanner(
    scanUi: CatalogScanUi,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // Surface draws under the status bar; only content is padded.
    val statusBarTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp,
    ) {
        if (scanUi.active) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarTop)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.scanning_folders_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = formatCatalogScanDetail(scanUi.detail),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    TextButton(onClick = onCancel) {
                        Text(stringResource(Res.string.scan_banner_cancel))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (scanUi.total > 0) {
                    LinearProgressIndicator(
                        progress = { scanUi.processed.toFloat() / scanUi.total.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            Res.string.scan_files_progress,
                            scanUi.processed,
                            scanUi.total,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun formatCatalogScanDetail(detail: CatalogScanDetail): String = when (detail) {
    CatalogScanDetail.Preparing -> stringResource(Res.string.scan_detail_preparing)
    CatalogScanDetail.CollectingFiles -> stringResource(Res.string.scan_detail_collecting_files)
    CatalogScanDetail.ReadingMetadata -> stringResource(Res.string.scan_detail_reading_metadata)
}

@Composable
fun formatCatalogScanSummary(summary: CatalogScanSummary): String = when (summary) {
    CatalogScanSummary.CannotOpenFolder ->
        stringResource(Res.string.scan_summary_cannot_open_folder)

    CatalogScanSummary.CannotReadFolder ->
        stringResource(Res.string.scan_summary_cannot_read_folder)

    CatalogScanSummary.NoFilesSeenAccessIssue ->
        stringResource(Res.string.scan_summary_no_files_seen)

    is CatalogScanSummary.UpdatedBooks ->
        stringResource(Res.string.scan_summary_updated_books, summary.books, summary.files)

    CatalogScanSummary.ScanFailed ->
        stringResource(Res.string.scan_summary_failed)

    CatalogScanSummary.Cancelled ->
        stringResource(Res.string.scan_summary_cancelled)

    CatalogScanSummary.NoFoldersToScan ->
        stringResource(Res.string.scan_summary_no_folders_to_scan)

    is CatalogScanSummary.CannotOpenFolders ->
        stringResource(
            Res.string.scan_summary_cannot_open_folders,
            summary.names.joinToString(),
        )

    CatalogScanSummary.NothingToScan ->
        stringResource(Res.string.scan_summary_nothing_to_scan)

    is CatalogScanSummary.NoFilesReadAccess ->
        stringResource(
            Res.string.scan_summary_no_files_read_access,
            summary.folderNames.joinToString(),
        )

    CatalogScanSummary.NothingIndexed ->
        stringResource(Res.string.scan_summary_nothing_indexed)

    is CatalogScanSummary.Indexed -> {
        val base = stringResource(
            Res.string.scan_summary_indexed,
            summary.books,
            summary.files,
        )
        val suffix = when {
            summary.problemFolders.isEmpty() -> ""
            summary.problemFolders.size <= 2 -> stringResource(
                Res.string.scan_summary_skipped_or_failed_names,
                summary.problemFolders.joinToString(),
            )

            else -> stringResource(
                Res.string.scan_summary_skipped_or_failed_count,
                summary.problemFolders.size,
            )
        }
        base + suffix
    }
}
