package by.tigre.music.player.core.presentation.playlist.library.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.zIndex
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.media.platform.tools.platform.compose.view.CardWithPopup
import by.tigre.media.platform.tools.platform.compose.view.CoverThumbnail
import by.tigre.media.platform.tools.platform.compose.view.EmptyScreen
import by.tigre.media.platform.tools.platform.compose.view.ErrorScreen
import by.tigre.media.platform.tools.platform.compose.view.PopupAction
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicator
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicatorSize
import by.tigre.media.platform.tools.platform.compose.view.bottomBarListContentPadding
import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.entiry.playlist.PlaylistTrackEntry
import by.tigre.music.player.core.presentation.playlist.library.component.PlaylistDetailComponent
import by.tigre.music.player.core.presentation.playlist.library.component.PlaylistDetailComponent.Message
import `by`.tigre.music.player.core.presentation.playlist.library.resources.Res
import `by`.tigre.music.player.core.presentation.playlist.library.resources.*
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

class PlaylistDetailView(
    private val component: PlaylistDetailComponent,
    private val albumArtProvider: AlbumArtProvider,
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
        var topMenuOpened by remember { mutableStateOf(false) }

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
                            Text(stringResource(Res.string.playlist_play))
                        }
                        Box {
                            IconButton(onClick = { topMenuOpened = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(Res.string.playlist_menu_more),
                                )
                            }
                            DropdownMenu(
                                expanded = topMenuOpened,
                                onDismissRequest = { topMenuOpened = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.playlist_rename)) },
                                    onClick = {
                                        topMenuOpened = false
                                        showRenameDialog = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.playlist_delete_confirm)) },
                                    onClick = {
                                        topMenuOpened = false
                                        showDeleteDialog = true
                                    },
                                )
                            }
                        }
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
                        tracks = state.value,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
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
                modifier = modifier.padding(horizontal = 32.dp),
                title = stringResource(Res.string.playlist_detail_empty_title),
                message = stringResource(Res.string.playlist_detail_empty_message),
                actionTitle = stringResource(Res.string.playlist_detail_add_tracks),
                icon = Icons.Outlined.PlaylistPlay,
                reloadAction = component::onAddTracksClicked,
            )
            return
        }

        val listState = rememberLazyListState()

        var localTracks by remember(tracks) { mutableStateOf(tracks) }
        LaunchedEffect(tracks) {
            localTracks = tracks
        }

        val originalOrder = remember(tracks) { tracks.map { it.entryId } }

        fun commitReorderIfNeeded() {
            val newOrder = localTracks.map { it.entryId }
            if (newOrder != originalOrder) {
                component.onTracksReordered(newOrder)
            }
        }

        val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
            localTracks = localTracks.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        }

        LazyColumn(
            modifier = modifier,
            state = listState,
            contentPadding = bottomBarListContentPadding(horizontal = 0.dp, top = 16.dp, extraBottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            itemsIndexed(localTracks, key = { _, track -> track.entryId }) { index, track ->
                val song = track.song
                val title = if (song == null) {
                    "${index + 1}. ${stringResource(Res.string.playlist_track_unavailable)}"
                } else {
                    val trackInAlbum = song.index.trim()
                    val albumTrackPart = if (trackInAlbum.isNotEmpty()) "($trackInAlbum) - " else ""
                    "${index + 1}. - $albumTrackPart${song.name}"
                }
                val descriptions = if (song == null) {
                    emptyList()
                } else {
                    listOf(stringResource(Res.string.playlist_track_meta, song.artist, song.album))
                }
                ReorderableItem(reorderableState, key = track.entryId) { isDragging ->
                    CardWithPopup(
                        modifier = Modifier.zIndex(if (isDragging) 1f else 0f),
                        title = title,
                        onCardClicked = { },
                        descriptions = descriptions,
                        popupActions = buildList {
                            if (song != null) {
                                add(
                                    PopupAction(stringResource(Res.string.playlist_open_artist)) {
                                        component.onOpenArtist(track)
                                    }
                                )
                                add(
                                    PopupAction(stringResource(Res.string.playlist_open_album)) {
                                        component.onOpenAlbum(track)
                                    }
                                )
                            }
                            if (index > 0) {
                                add(
                                    PopupAction(stringResource(Res.string.playlist_move_up)) {
                                        component.onMoveTrackUp(track.entryId)
                                    }
                                )
                                add(
                                    PopupAction(stringResource(Res.string.playlist_move_to_top)) {
                                        component.onMoveTrackToTop(track.entryId)
                                    }
                                )
                            }
                            if (index < localTracks.lastIndex) {
                                add(
                                    PopupAction(stringResource(Res.string.playlist_move_down)) {
                                        component.onMoveTrackDown(track.entryId)
                                    }
                                )
                                add(
                                    PopupAction(stringResource(Res.string.playlist_move_to_bottom)) {
                                        component.onMoveTrackToBottom(track.entryId)
                                    }
                                )
                            }
                            add(
                                PopupAction(stringResource(Res.string.playlist_remove_track)) {
                                    component.onRemoveTrack(track)
                                }
                            )
                        },
                        leadingContent = if (song != null) {
                            {
                                CoverThumbnail(model = albumArtProvider.albumArtUri(song.albumId))
                            }
                        } else {
                            null
                        },
                        menuModifier = Modifier.longPressDraggableHandle(
                            onDragStopped = { commitReorderIfNeeded() },
                        ),
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
