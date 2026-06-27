package by.tigre.music.player.core.presentation.playlist.library.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.media.platform.tools.platform.compose.view.CardWithPopup
import by.tigre.media.platform.tools.platform.compose.view.EmptyScreen
import by.tigre.media.platform.tools.platform.compose.view.ErrorScreen
import by.tigre.media.platform.tools.platform.compose.view.LocalBottomBarHeight
import by.tigre.media.platform.tools.platform.compose.view.PopupAction
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicator
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicatorSize
import by.tigre.media.platform.tools.platform.compose.view.bottomBarListContentPadding
import by.tigre.music.player.core.entiry.playlist.Playlist
import by.tigre.music.player.core.presentation.playlist.library.component.PlaylistsListComponent
import by.tigre.music.player.core.presentation.playlist.library.component.PlaylistsListComponent.PlaylistsDialogState
import `by`.tigre.music.player.core.presentation.playlist.library.resources.Res
import `by`.tigre.music.player.core.presentation.playlist.library.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

class PlaylistsListView(
    private val component: PlaylistsListComponent
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        val screenState by component.screenState.collectAsState()
        val dialogState by component.dialogState.collectAsState()
        val nameError by component.nameError.collectAsState()
        val bottomBarHeight = LocalBottomBarHeight.current
        val nameTakenMessage = stringResource(Res.string.playlist_name_taken)

        DrawDialog(dialogState, nameError, nameTakenMessage)

        Box(modifier = modifier) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = stringResource(Res.string.nav_playlists),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                },
            ) { paddingValues ->
                when (val state = screenState) {
                    is ScreenContentState.Loading -> {
                        ProgressIndicator(
                            Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            ProgressIndicatorSize.LARGE,
                        )
                    }

                    is ScreenContentState.Error -> {
                        ErrorScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            retryAction = component::retry,
                        )
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

            val playlists = (screenState as? ScreenContentState.Content)?.value
            if (!playlists.isNullOrEmpty()) {
                FloatingActionButton(
                    onClick = component::onCreateClicked,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = bottomBarHeight + 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(Res.string.playlist_create),
                    )
                }
            }
        }
    }

    @Composable
    private fun DrawContent(playlists: List<Playlist>, modifier: Modifier) {
        if (playlists.isEmpty()) {
            EmptyScreen(
                modifier = modifier.padding(horizontal = 32.dp),
                message = stringResource(Res.string.playlists_empty),
                reloadAction = component::onCreateClicked,
                actionTitle = stringResource(Res.string.playlist_create),
                icon = Icons.Outlined.PlaylistPlay,
            )
            return
        }

        LazyColumn(
            modifier = modifier,
            contentPadding = bottomBarListContentPadding(top = 16.dp, extraBottom = 72.dp),
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
                            pluralStringResource(
                                Res.plurals.playlist_track_count,
                                playlist.trackCount,
                                playlist.trackCount,
                            ),
                        ),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PlaylistPlay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun DrawDialog(
        dialogState: PlaylistsDialogState?,
        nameError: Boolean,
        nameTakenMessage: String,
    ) {
        when (dialogState) {
            PlaylistsDialogState.Create -> {
                NameInputDialog(
                    title = stringResource(Res.string.playlist_create),
                    confirmTitle = stringResource(Res.string.playlist_create),
                    initialName = "",
                    errorMessage = if (nameError) nameTakenMessage else null,
                    onConfirm = component::onCreateConfirmed,
                    onDismiss = component::dismissDialog,
                )
            }

            is PlaylistsDialogState.Rename -> {
                NameInputDialog(
                    title = stringResource(Res.string.playlist_rename),
                    confirmTitle = stringResource(Res.string.playlist_rename),
                    initialName = dialogState.playlist.name,
                    errorMessage = if (nameError) nameTakenMessage else null,
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
        errorMessage: String?,
        onConfirm: (String) -> Unit,
        onDismiss: () -> Unit,
    ) {
        var textValue by remember(initialName) { mutableStateOf(initialName) }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        singleLine = true,
                        isError = errorMessage != null,
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
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
