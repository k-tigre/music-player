package by.tigre.music.player.core.presentation.playlist.library.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.media.platform.tools.platform.compose.view.EmptyScreen
import by.tigre.media.platform.tools.platform.compose.view.ErrorScreen
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicator
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicatorSize
import by.tigre.media.platform.tools.platform.compose.view.bottomBarListContentPadding
import by.tigre.music.player.core.entiry.playlist.PlaylistTrackEntry
import by.tigre.music.player.core.presentation.playlist.library.component.PlaylistDetailComponent
import by.tigre.music.player.core.presentation.playlist.library.component.PlaylistDetailComponent.Message
import `by`.tigre.music.player.core.presentation.playlist.library.resources.Res
import `by`.tigre.music.player.core.presentation.playlist.library.resources.*
import org.jetbrains.compose.resources.stringResource

class PlaylistDetailView(
    private val component: PlaylistDetailComponent,
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        val screenState by component.screenState.collectAsState()
        val playlistName by component.playlistName.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val noPlayableTracksText = stringResource(Res.string.playlist_no_playable_tracks)

        var showRenameDialog by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            component.messages.collect { message ->
                when (message) {
                    Message.NoPlayableTracks -> snackbarHostState.showSnackbar(noPlayableTracksText)
                }
            }
        }

        if (showRenameDialog) {
            RenamePlaylistDialog(
                initialName = playlistName,
                onDismiss = { showRenameDialog = false },
                onConfirm = { newName ->
                    component.onRename(newName)
                    showRenameDialog = false
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(playlistName) },
                text = { Text(stringResource(Res.string.playlist_delete_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        component.onDeletePlaylist()
                    }) {
                        Text(stringResource(Res.string.playlist_delete_confirm))
                    }
                },
                dismissButton = {},
            )
        }

        Scaffold(
            modifier = modifier,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = playlistName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = component::onBackClicked) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.nav_playlists),
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = component::onPlayAll) {
                            Text(stringResource(Res.string.playlist_play_all))
                        }
                        TextButton(onClick = component::onAddAllToQueue) {
                            Text(stringResource(Res.string.playlist_add_all_to_queue))
                        }
                        IconButton(onClick = { showRenameDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(Res.string.playlist_rename),
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(Res.string.playlist_delete_confirm),
                            )
                        }
                        IconButton(onClick = component::retry) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(Res.string.nav_playlists),
                            )
                        }
                    },
                )
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
                        tracks = state.value,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
            }
        }
    }

    @Composable
    private fun DrawContent(
        tracks: List<PlaylistTrackEntry>,
        modifier: Modifier,
    ) {
        if (tracks.isEmpty()) {
            EmptyScreen(
                modifier = modifier.padding(32.dp),
                message = stringResource(Res.string.playlists_empty),
                reloadAction = component::retry,
            )
            return
        }

        LazyColumn(
            modifier = modifier,
            contentPadding = bottomBarListContentPadding(top = 16.dp, extraBottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tracks.forEachIndexed { index, track ->
                item(key = track.entryId) {
                    TrackRow(
                        position = index + 1,
                        track = track,
                        canMoveUp = index > 0,
                        canMoveDown = index < tracks.lastIndex,
                    )
                }
            }
        }
    }

    @Composable
    private fun TrackRow(
        position: Int,
        track: PlaylistTrackEntry,
        canMoveUp: Boolean,
        canMoveDown: Boolean,
    ) {
        val song = track.song
        val unavailableColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        var menuOpened by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = song != null) { component.onPlayTrack(track) },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (song == null) {
                            "$position. ${stringResource(Res.string.playlist_track_unavailable)}"
                        } else {
                            "$position. ${song.name}"
                        },
                        color = if (song == null) unavailableColor else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (song != null) {
                        Text(
                            text = "${song.artist} / ${song.album}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.size(8.dp))
                IconButton(
                    enabled = canMoveUp,
                    onClick = { component.onMoveTrackUp(track.entryId) },
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                    )
                }
                IconButton(
                    enabled = canMoveDown,
                    onClick = { component.onMoveTrackDown(track.entryId) },
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                    )
                }
                IconButton(onClick = { menuOpened = true }) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = null)
                }
                DropdownMenu(
                    expanded = menuOpened,
                    onDismissRequest = { menuOpened = false },
                ) {
                    if (song != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.playlist_play_all)) },
                            onClick = {
                                menuOpened = false
                                component.onPlayTrack(track)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.playlist_add_all_to_queue)) },
                            onClick = {
                                menuOpened = false
                                component.onAddTrackToQueue(track)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.playlist_open_artist)) },
                            onClick = {
                                menuOpened = false
                                component.onOpenArtist(track)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.playlist_open_album)) },
                            onClick = {
                                menuOpened = false
                                component.onOpenAlbum(track)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.playlist_remove_track)) },
                        onClick = {
                            menuOpened = false
                            component.onRemoveTrack(track)
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun RenamePlaylistDialog(
        initialName: String,
        onDismiss: () -> Unit,
        onConfirm: (String) -> Unit,
    ) {
        var value by remember(initialName) { mutableStateOf(initialName) }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.playlist_rename)) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(value) }) {
                    Text(stringResource(Res.string.playlist_rename))
                }
            },
            dismissButton = {},
        )
    }
}
