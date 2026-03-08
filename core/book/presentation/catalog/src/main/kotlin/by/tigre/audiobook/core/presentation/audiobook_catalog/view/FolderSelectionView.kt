package by.tigre.audiobook.core.presentation.audiobook_catalog.view

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import by.tigre.audiobook.core.entity.catalog.FolderSource
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.FolderSelectionComponent
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.tools.platform.compose.ComposableView
import by.tigre.music.player.tools.platform.compose.view.ErrorScreen
import by.tigre.music.player.tools.platform.compose.view.ProgressIndicator
import by.tigre.music.player.tools.platform.compose.view.ProgressIndicatorSize

class FolderSelectionView(
    private val component: FolderSelectionComponent
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->
            uri?.let {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, flags)

                val docFile = DocumentFile.fromTreeUri(context, it)
                val name = docFile?.name ?: "Unknown"

                component.onFolderSelected(it.toString(), name)
            }
        }

        val isScanning by component.isScanning.collectAsState()

        if (isScanning) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("Scanning folders") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Please wait...")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Audiobook Folders",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        IconButton(onClick = component::onRescanFolders, enabled = !isScanning) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Rescan folders"
                            )
                        }
                        TextButton(onClick = component::onNavigateToBooks) {
                            Text("Books")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { launcher.launch(null) }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add folder"
                    )
                }
            },
            content = { paddingValues ->
                val screenState by component.screenState.collectAsState()

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
                            DrawContent(state.value)
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun DrawContent(folders: List<FolderSource>) {
        if (folders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No folders added yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Tap + to select a folder with audiobooks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
                                }

                                IconButton(onClick = { component.onRemoveFolder(folder.id) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Remove folder"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
