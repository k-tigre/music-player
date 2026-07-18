package by.tigre.audiobook.core.presentation.audiobook_catalog.view

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import by.tigre.audiobook.core.data.audiobook.CatalogScanDetail
import by.tigre.audiobook.core.data.audiobook.CatalogScanSummary
import by.tigre.audiobook.core.data.audiobook.FolderSourceAccessHealth
import by.tigre.audiobook.core.entity.catalog.FolderSource
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.FolderSelectionComponent
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.media.platform.tools.platform.compose.view.ErrorScreen
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicator
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicatorSize
import by.tigre.media.platform.tools.platform.compose.view.bottomBarListContentPadding
import by.tigre.media.platform.tools.platform.compose.view.centeredScreenContentBottomPadding
import `by`.tigre.audiobook.core.presentation.catalog.resources.Res
import `by`.tigre.audiobook.core.presentation.catalog.resources.cd_add_folder
import `by`.tigre.audiobook.core.presentation.catalog.resources.cd_remove_folder
import `by`.tigre.audiobook.core.presentation.catalog.resources.cd_rescan_folders
import `by`.tigre.audiobook.core.presentation.catalog.resources.folder_health_cannot_list
import `by`.tigre.audiobook.core.presentation.catalog.resources.folder_health_empty_but_indexed
import `by`.tigre.audiobook.core.presentation.catalog.resources.folder_health_tree_uri_unavailable
import `by`.tigre.audiobook.core.presentation.catalog.resources.folder_persist_permission_error
import `by`.tigre.audiobook.core.presentation.catalog.resources.folder_selection_title
import `by`.tigre.audiobook.core.presentation.catalog.resources.folder_unknown_name
import `by`.tigre.audiobook.core.presentation.catalog.resources.folders_access_broke_hint
import `by`.tigre.audiobook.core.presentation.catalog.resources.folders_empty_action
import `by`.tigre.audiobook.core.presentation.catalog.resources.folders_empty_hint
import `by`.tigre.audiobook.core.presentation.catalog.resources.folders_empty_title
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_detail_collecting_files
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_detail_preparing
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_detail_reading_metadata
import `by`.tigre.audiobook.core.presentation.catalog.resources.scan_files_progress
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

class FolderSelectionView(
    private val component: FolderSelectionComponent
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val folderPersistErrorMessage = stringResource(Res.string.folder_persist_permission_error)
        val unknownFolderName = stringResource(Res.string.folder_unknown_name)

        val treeLauncher = rememberLauncherForActivityResult(
            contract = OpenAudiobookFolderContract()
        ) { uri: Uri? ->
            uri?.let {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                try {
                    context.contentResolver.takePersistableUriPermission(it, flags)
                } catch (_: SecurityException) {
                    scope.launch {
                        snackbarHostState.showSnackbar(folderPersistErrorMessage)
                    }
                    return@rememberLauncherForActivityResult
                }

                val docFile = DocumentFile.fromTreeUri(context, it)
                val name = docFile?.name ?: unknownFolderName

                component.onFolderSelected(it.toString(), name)
            }
        }

        val scanUi by component.catalogScanUi.collectAsState()
        val completedSummaryText = scanUi.completedSummary?.let { formatScanSummary(it) }
        var wasScanActive by remember { mutableStateOf(false) }
        LaunchedEffect(scanUi.active, completedSummaryText) {
            if (wasScanActive && !scanUi.active) {
                completedSummaryText?.let { snackbarHostState.showSnackbar(it) }
                component.refreshFolderAccessHealth()
            }
            wasScanActive = scanUi.active
        }

        if (scanUi.active) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text(stringResource(Res.string.scanning_folders_title)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (scanUi.total > 0) {
                            LinearProgressIndicator(
                                progress = { scanUi.processed.toFloat() / scanUi.total.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(Res.string.scan_files_progress, scanUi.processed, scanUi.total))
                        } else {
                            CircularProgressIndicator()
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(formatScanDetail(scanUi.detail))
                    }
                }
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.folder_selection_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = component::onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = component::onRescanFolders, enabled = !scanUi.active) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(Res.string.cd_rescan_folders)
                            )
                        }
                        IconButton(
                            onClick = { treeLauncher.launch(null) },
                            enabled = !scanUi.active,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(Res.string.cd_add_folder)
                            )
                        }
                    }
                )
            },
            content = { paddingValues ->
                val screenState by component.screenState.collectAsState()
                val folderHealth by component.folderAccessHealth.collectAsState()

                AnimatedContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    targetState = screenState,
                    label = "state",
                    contentKey = { state -> state::class.java },
                ) { state ->
                    when (state) {
                        is ScreenContentState.Loading -> {
                            ProgressIndicator(Modifier.fillMaxSize(), ProgressIndicatorSize.LARGE)
                        }

                        is ScreenContentState.Error -> {
                            ErrorScreen(retryAction = component::retry)
                        }

                        is ScreenContentState.Content -> {
                            DrawContent(
                                folders = state.value,
                                health = folderHealth,
                                onAddFolder = { treeLauncher.launch(null) },
                            )
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun DrawContent(
        folders: List<FolderSource>,
        health: Map<FolderSource.Id, FolderSourceAccessHealth>,
        onAddFolder: () -> Unit,
    ) {
        if (folders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .centeredScreenContentBottomPadding()
                    .padding(48.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(Res.string.folders_empty_title),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = stringResource(Res.string.folders_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onAddFolder,
                    modifier = Modifier.padding(top = 24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.folders_empty_action))
                }
            }
        } else {
            val anyIssue = folders.any { folder ->
                health[folder.id] != null && health[folder.id] != FolderSourceAccessHealth.Ok
            }
            LazyColumn(
                contentPadding = bottomBarListContentPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (anyIssue) {
                    item {
                        Text(
                            text = stringResource(Res.string.folders_access_broke_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                items(folders, key = { it.id.value }) { folder ->
                    FolderSourceCard(
                        folder = folder,
                        healthHint = accessHealthHintText(health[folder.id]),
                        onRemove = { component.onRemoveFolder(folder.id) },
                    )
                }
            }
        }
    }

    @Composable
    private fun FolderSourceCard(
        folder: FolderSource,
        healthHint: String?,
        onRemove: () -> Unit,
    ) {
        val shape = RoundedCornerShape(12.dp)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = shape,
                ),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    healthHint?.let { hint ->
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(Res.string.cd_remove_folder),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    @Composable
    private fun formatScanDetail(detail: CatalogScanDetail): String {
        return when (detail) {
            CatalogScanDetail.Preparing -> stringResource(Res.string.scan_detail_preparing)
            CatalogScanDetail.CollectingFiles -> stringResource(Res.string.scan_detail_collecting_files)
            CatalogScanDetail.ReadingMetadata -> stringResource(Res.string.scan_detail_reading_metadata)
        }
    }

    @Composable
    private fun formatScanSummary(summary: CatalogScanSummary): String {
        return when (summary) {
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
    }

    @Composable
    private fun accessHealthHintText(health: FolderSourceAccessHealth?): String? {
        return when (health) {
            null, FolderSourceAccessHealth.Ok -> null
            FolderSourceAccessHealth.TreeUriUnavailable ->
                stringResource(Res.string.folder_health_tree_uri_unavailable)

            FolderSourceAccessHealth.CannotListContents ->
                stringResource(Res.string.folder_health_cannot_list)

            FolderSourceAccessHealth.ListedButEmptyWithIndexedBooks ->
                stringResource(Res.string.folder_health_empty_but_indexed)
        }
    }
}
