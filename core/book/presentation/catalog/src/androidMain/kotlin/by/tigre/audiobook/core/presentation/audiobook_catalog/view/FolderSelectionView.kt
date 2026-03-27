package by.tigre.audiobook.core.presentation.audiobook_catalog.view

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.tools.platform.compose.ComposableView
import by.tigre.music.player.tools.platform.compose.view.ErrorScreen
import by.tigre.music.player.tools.platform.compose.view.ProgressIndicator
import by.tigre.music.player.tools.platform.compose.view.ProgressIndicatorSize
import `by`.tigre.audiobook.core.presentation.catalog.resources.Res
import `by`.tigre.audiobook.core.presentation.catalog.resources.*
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
        var wasScanActive by remember { mutableStateOf(false) }
        LaunchedEffect(scanUi.active, scanUi.completedSummary) {
            if (wasScanActive && !scanUi.active) {
                if (scanUi.completedSummary.isNotEmpty()) {
                    snackbarHostState.showSnackbar(scanUi.completedSummary)
                }
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
                        Text(scanUi.detail)
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
                    actions = {
                        IconButton(onClick = component::onRescanFolders, enabled = !scanUi.active) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(Res.string.cd_rescan_folders)
                            )
                        }
                        TextButton(onClick = component::onNavigateToBooks) {
                            Text(stringResource(Res.string.nav_books))
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { treeLauncher.launch(null) }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(Res.string.cd_add_folder)
                    )
                }
            },
            content = { paddingValues ->
                val screenState by component.screenState.collectAsState()
                val folderHealth by component.folderAccessHealth.collectAsState()

                Crossfade(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    targetState = screenState,
                    animationSpec = tween(500),
                    label = "state"
                ) { state ->
                    when (state) {
                        is ScreenContentState.Loading -> {
                            ProgressIndicator(Modifier.fillMaxSize(), ProgressIndicatorSize.LARGE)
                        }

                        is ScreenContentState.Error -> {
                            ErrorScreen(retryAction = component::retry)
                        }

                        is ScreenContentState.Content -> {
                            DrawContent(state.value, folderHealth)
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
    ) {
        if (folders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val anyIssue = folders.any { folder ->
                health[folder.id] != null && health[folder.id] != FolderSourceAccessHealth.Ok
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (anyIssue) {
                    item {
                        Text(
                            text = stringResource(Res.string.folders_access_broke_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                folders.forEach { folder ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    accessHealthHintText(health[folder.id])?.let { hint ->
                                        Text(
                                            text = hint,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }

                                IconButton(onClick = { component.onRemoveFolder(folder.id) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(Res.string.cd_remove_folder)
                                    )
                                }
                            }
                        }
                    }
                }
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
