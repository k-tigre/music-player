package by.tigre.music.player.core.presentation.playlist.library.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.media.platform.tools.platform.compose.view.CardWithPopup
import by.tigre.media.platform.tools.platform.compose.view.EmptyScreen
import by.tigre.media.platform.tools.platform.compose.view.ErrorScreen
import by.tigre.media.platform.tools.platform.compose.view.PopupAction
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicator
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicatorSize
import by.tigre.media.platform.tools.platform.compose.view.bottomBarListContentPadding
import by.tigre.music.player.core.entiry.playlist.Playlist
import by.tigre.music.player.core.presentation.playlist.library.component.PlaylistsListComponent
import by.tigre.music.player.core.presentation.playlist.library.component.PlaylistsListComponent.PlaylistsDialogState
import `by`.tigre.music.player.core.presentation.playlist.library.resources.Res
import `by`.tigre.music.player.core.presentation.playlist.library.resources.*
import org.jetbrains.compose.resources.stringResource

class PlaylistsListView(
    private val component: PlaylistsListComponent
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        val screenState by component.screenState.collectAsState()
        val dialogState by component.dialogState.collectAsState()

        DrawDialog(dialogState)

        Scaffold(
            modifier = modifier,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.nav_playlists),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    actions = {
                        IconButton(onClick = component::retry) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(Res.string.nav_playlists),
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = component::onCreateClicked) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(Res.string.playlist_create),
                    )
                }
            },
        ) { paddingValues ->
            when (val state = screenState) {
                is ScreenContentState.Loading -> {
                    ProgressIndicator(Modifier.fillMaxSize(), ProgressIndicatorSize.LARGE)
                }

                is ScreenContentState.Error -> {
                    ErrorScreen(retryAction = component::retry)
                }

                is ScreenContentState.Content -> {
                    DrawContent(
                        playlists = state.value,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    )
                }
            }
        }
    }

    @Composable
    private fun DrawContent(playlists: List<Playlist>, modifier: Modifier) {
        if (playlists.isEmpty()) {
            EmptyScreen(
                modifier = modifier.padding(32.dp),
                message = stringResource(Res.string.playlists_empty),
                reloadAction = component::onCreateClicked,
                actionTitle = stringResource(Res.string.playlist_create),
            )
            return
        }

        LazyColumn(
            modifier = modifier,
            contentPadding = bottomBarListContentPadding(top = 16.dp, extraBottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            playlists.forEach { playlist ->
                item(key = playlist.id.value) {
                    CardWithPopup(
                        modifier = Modifier,
                        title = playlist.name,
                        onCardClicked = { component.onPlaylistClicked(playlist) },
                        popupActions = listOf(
                            PopupAction(stringResource(Res.string.playlist_rename)) {
                                component.onRenameClicked(playlist)
                            },
                            PopupAction(stringResource(Res.string.playlist_delete_confirm)) {
                                component.onDeleteClicked(playlist)
                            },
                        ),
                        descriptions = listOf(
                            "${playlist.trackCount}",
                        ),
                    )
                }
            }
        }
    }

    @Composable
    private fun DrawDialog(dialogState: PlaylistsDialogState?) {
        when (dialogState) {
            PlaylistsDialogState.Create -> {
                NameInputDialog(
                    title = stringResource(Res.string.playlist_create),
                    confirmTitle = stringResource(Res.string.playlist_create),
                    initialName = "",
                    onConfirm = component::onCreateConfirmed,
                    onDismiss = component::dismissDialog,
                )
            }

            is PlaylistsDialogState.Rename -> {
                NameInputDialog(
                    title = stringResource(Res.string.playlist_rename),
                    confirmTitle = stringResource(Res.string.playlist_rename),
                    initialName = dialogState.playlist.name,
                    onConfirm = component::onRenameConfirmed,
                    onDismiss = component::dismissDialog,
                )
            }

            is PlaylistsDialogState.Delete -> {
                AlertDialog(
                    onDismissRequest = component::dismissDialog,
                    title = {
                        Text(text = dialogState.playlist.name)
                    },
                    text = {
                        Text(stringResource(Res.string.playlist_delete_confirm))
                    },
                    confirmButton = {
                        TextButton(onClick = component::onDeleteConfirmed) {
                            Text(stringResource(Res.string.playlist_delete_confirm))
                        }
                    },
                    dismissButton = {},
                )
            }

            null -> Unit
        }
    }

    @Composable
    private fun NameInputDialog(
        title: String,
        confirmTitle: String,
        initialName: String,
        onConfirm: (String) -> Unit,
        onDismiss: () -> Unit,
    ) {
        var textValue by remember(initialName) { mutableStateOf(initialName) }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(textValue) }) {
                    Text(confirmTitle)
                }
            },
            dismissButton = {},
        )
    }
}
