package by.tigre.audiobook.core.presentation.audiobook_catalog.view

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import by.tigre.audiobook.core.data.audiobook.FolderSourceAccessHealth
import by.tigre.audiobook.core.entity.catalog.FolderSource
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.FolderSelectionComponent
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.media.platform.tools.platform.compose.appTopBarWindowInsets
import by.tigre.media.platform.tools.platform.compose.view.ErrorScreen
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicator
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicatorSize
import by.tigre.media.platform.tools.platform.compose.view.bottomBarListContentPadding
import by.tigre.media.platform.tools.platform.compose.view.centeredScreenContentBottomPadding
import `by`.tigre.audiobook.core.presentation.catalog.resources.Res
import `by`.tigre.audiobook.core.presentation.catalog.resources.cd_add_folder
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
import `by`.tigre.audiobook.core.presentation.catalog.resources.folders_remove_confirm_body
import `by`.tigre.audiobook.core.presentation.catalog.resources.folders_remove_confirm_cancel
import `by`.tigre.audiobook.core.presentation.catalog.resources.folders_remove_confirm_title
import `by`.tigre.audiobook.core.presentation.catalog.resources.folders_remove_from_library
import `by`.tigre.audiobook.core.presentation.catalog.resources.folders_shared_banner
import `by`.tigre.audiobook.core.presentation.catalog.resources.folders_source_subtitle
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
        var pendingRemove by remember { mutableStateOf<FolderSelectionComponent.FolderSourceRow?>(null) }

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
        var wasScanActive by remember { mutableStateOf(false) }
        LaunchedEffect(scanUi.active) {
            if (wasScanActive && !scanUi.active) {
                component.refreshFolderAccessHealth()
            }
            wasScanActive = scanUi.active
        }

        pendingRemove?.let { row ->
            AlertDialog(
                onDismissRequest = { pendingRemove = null },
                title = { Text(stringResource(Res.string.folders_remove_confirm_title)) },
                text = {
                    Text(
                        stringResource(Res.string.folders_remove_confirm_body, row.folder.name),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            component.onRemoveFolder(row.folder.id)
                            pendingRemove = null
                        },
                    ) {
                        Text(
                            text = stringResource(Res.string.folders_remove_from_library),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRemove = null }) {
                        Text(stringResource(Res.string.folders_remove_confirm_cancel))
                    }
                },
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
                    windowInsets = appTopBarWindowInsets(),
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
                                rows = state.value,
                                health = folderHealth,
                                onAddFolder = { treeLauncher.launch(null) },
                                onRequestRemove = { pendingRemove = it },
                            )
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun DrawContent(
        rows: List<FolderSelectionComponent.FolderSourceRow>,
        health: Map<FolderSource.Id, FolderSourceAccessHealth>,
        onAddFolder: () -> Unit,
        onRequestRemove: (FolderSelectionComponent.FolderSourceRow) -> Unit,
    ) {
        if (rows.isEmpty()) {
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
            val anyIssue = rows.any { row ->
                health[row.folder.id] != null && health[row.folder.id] != FolderSourceAccessHealth.Ok
            }
            LazyColumn(
                contentPadding = bottomBarListContentPadding(),
            ) {
                item {
                    Text(
                        text = stringResource(Res.string.folders_shared_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    HorizontalDivider()
                }
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
                items(rows, key = { it.folder.id.value }) { row ->
                    FolderSourceRow(
                        row = row,
                        healthHint = accessHealthHintText(health[row.folder.id]),
                        onRemove = { onRequestRemove(row) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }

    @Composable
    private fun FolderSourceRow(
        row: FolderSelectionComponent.FolderSourceRow,
        healthHint: String?,
        onRemove: () -> Unit,
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = row.folder.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Column {
                    Text(
                        text = stringResource(
                            Res.string.folders_source_subtitle,
                            row.bookCount,
                        ),
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
            },
            trailingContent = {
                TextButton(onClick = onRemove) {
                    Text(
                        text = stringResource(Res.string.folders_remove_from_library),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
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
