package by.tigre.music.player.core.presentation.playlist.current.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Save
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.entiry.playback.NowPlayingQueueEntry
import by.tigre.music.player.core.entiry.playback.NowPlayingScreenModel
import by.tigre.music.player.core.entiry.playback.OverlayQueueEntry
import by.tigre.music.player.core.entiry.playback.QueueSession
import by.tigre.music.player.core.entiry.playback.SongInQueueItem
import by.tigre.music.player.core.presentation.playlist.current.component.CurrentQueueComponent
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.media.platform.tools.platform.compose.view.CoverThumbnail
import by.tigre.media.platform.tools.platform.compose.view.CardWithPopup
import by.tigre.media.platform.tools.platform.compose.view.bottomBarListContentPadding
import by.tigre.media.platform.tools.platform.compose.view.smartScrollToItem
import by.tigre.media.platform.tools.platform.compose.view.EmptyScreen
import by.tigre.media.platform.tools.platform.compose.view.ErrorScreen
import by.tigre.media.platform.tools.platform.compose.view.PopupAction
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicator
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicatorSize
import `by`.tigre.music.player.core.presentation.queue.resources.Res
import `by`.tigre.music.player.core.presentation.queue.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

class CurrentQueueView(
    private val component: CurrentQueueComponent,
    private val albumArtProvider: AlbumArtProvider,
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        val screenState by component.screenState.collectAsState()
        val saveDialogState by component.saveDialogState.collectAsState()
        val nameError by component.nameError.collectAsState()
        val contentModel = (screenState as? ScreenContentState.Content<NowPlayingScreenModel>)?.value

        saveDialogState?.let { dialogState ->
            SavePlaylistDialog(
                initialName = dialogState.defaultName,
                nameError = nameError,
                onDismiss = component::dismissSaveDialog,
                onConfirm = component::onSaveNewPlaylistConfirmed,
            )
        }

        Scaffold(
            modifier = modifier,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(Modifier.padding(horizontal = 48.dp)) {
                            Text(
                                text = queueTitleText(contentModel),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    actions = {
                        if (contentModel != null && showSaveButton(contentModel)) {
                            IconButton(onClick = component::onSaveClicked) {
                                Icon(
                                    imageVector = Icons.Outlined.Save,
                                    contentDescription = stringResource(Res.string.queue_save_playlist),
                                )
                            }
                        }
                    },
                )
            },
            content = { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    when (screenState) {
                        is ScreenContentState.Loading -> {
                            ProgressIndicator(Modifier.fillMaxSize(), ProgressIndicatorSize.LARGE)
                        }

                        is ScreenContentState.Error -> {
                            ErrorScreen(retryAction = component::retry)
                        }

                        is ScreenContentState.Content -> {
                            DrawContent((screenState as ScreenContentState.Content<NowPlayingScreenModel>).value)
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun queueTitleText(model: NowPlayingScreenModel?): String =
        when (val session = model?.session) {
            null, QueueSession.Plain -> stringResource(Res.string.screen_current_queue_title)
            is QueueSession.FromPlaylist -> {
                val playlistName = if (session.isDirty) "${session.name}*" else session.name
                stringResource(Res.string.queue_title_with_playlist, playlistName)
            }
        }

    private fun showSaveButton(model: NowPlayingScreenModel): Boolean =
        when (val session = model.session) {
            QueueSession.Plain -> model.queue.isNotEmpty()
            is QueueSession.FromPlaylist -> session.isDirty
        }

    @Composable
    private fun DrawContent(model: NowPlayingScreenModel) {
        if (model.overlay == null && model.queue.isEmpty()) {
            EmptyScreen(
                reloadAction = component::onAddToQueueClicked,
                title = stringResource(Res.string.queue_empty_title),
                message = stringResource(Res.string.queue_empty_message),
                actionTitle = stringResource(Res.string.queue_empty_action)
            )
        } else {
            val listState = rememberLazyListState()
            val nearMarginPx = with(LocalDensity.current) { 50.dp.toPx() }
            val currentModel by rememberUpdatedState(model)

            val queueIds = model.queue.map { it.id }

            var localQueue by remember { mutableStateOf(model.queue) }
            LaunchedEffect(queueIds) {
                if (localQueue.map { it.id } != queueIds) {
                    localQueue = model.queue
                }
            }
            LaunchedEffect(model.queue) {
                val currentIds = localQueue.map { it.id }
                val newIds = model.queue.map { it.id }
                if (currentIds == newIds) {
                    val byId = model.queue.associateBy { it.id }
                    localQueue = localQueue.map { entry -> byId[entry.id] ?: entry }
                }
            }

            val originalOrder = remember(queueIds) { queueIds }

            fun commitReorderIfNeeded() {
                val newOrder = localQueue.map { it.id }
                if (newOrder != originalOrder) {
                    component.onTracksReordered(newOrder)
                }
            }

            val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
                val offset = queueLazyOffset(currentModel)
                val fromIndex = from.index - offset
                val toIndex = to.index - offset
                if (fromIndex !in localQueue.indices || toIndex !in localQueue.indices) return@rememberReorderableLazyListState
                localQueue = localQueue.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }
            }

            suspend fun scrollToPlayingTrack(
                queueIndex: Int,
                screenModel: NowPlayingScreenModel,
                instant: Boolean,
                smoothDurationMillis: Int = 1200,
            ) {
                if (queueIndex < 0 || screenModel.overlay != null) return
                val targetIndex = lazyListIndexForQueueItem(queueIndex, screenModel)
                listState.awaitReadyForScroll(targetIndex)
                listState.smartScrollToItem(
                    targetIndex = targetIndex,
                    nearMarginPx = nearMarginPx,
                    instant = instant,
                    smoothDurationMillis = smoothDurationMillis,
                )
            }

            LaunchedEffect(model.overlay?.item?.uri) {
                if (model.overlay != null) {
                    listState.scrollToItem(0)
                } else {
                    val playingIndex = model.queue.indexOfFirst { it.isPlaying }
                    scrollToPlayingTrack(playingIndex, model, instant = true)
                }
            }
            LaunchedEffect(Unit) {
                snapshotFlow {
                    val playingIndex = currentModel.queue.indexOfFirst { it.isPlaying }
                    listState.layoutInfo.totalItemsCount to playingIndex
                }.first { (itemCount, playingIndex) ->
                    itemCount > 0 && playingIndex >= 0 && currentModel.overlay == null
                }
                val playingIndex = currentModel.queue.indexOfFirst { it.isPlaying }
                scrollToPlayingTrack(playingIndex, currentModel, instant = true)
            }
            LaunchedEffect(Unit) {
                component.scrollToPlayingTrackEvents.collect { queueIndex ->
                    scrollToPlayingTrack(queueIndex, currentModel, instant = false)
                }
            }
            LazyColumn(
                state = listState,
                contentPadding = bottomBarListContentPadding(horizontal = 0.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                model.overlay?.let { overlay ->
                    item(key = "section_now") {
                        SectionHeader(stringResource(Res.string.queue_section_now_playing))
                    }
                    item(key = "overlay") {
                        DrawOverlayRow(overlay, showReturnButton = model.queue.isNotEmpty())
                    }
                }

                if (localQueue.isNotEmpty()) {
                    if (model.overlay != null) {
                        item(key = "section_hold") {
                            SectionHeader(stringResource(Res.string.queue_section_on_hold))
                        }
                    }
                    itemsIndexed(localQueue, key = { _, entry -> entry.id }) { index, entry ->
                        ReorderableItem(reorderableState, key = entry.id) { isDragging ->
                            DrawQueueRow(
                                queuePositionOneBased = index + 1,
                                entry = entry,
                                overlayActive = model.overlay != null,
                                isDragging = isDragging,
                                canMoveUp = index > 0,
                                canMoveDown = index < localQueue.lastIndex,
                                menuDragModifier = Modifier.longPressDraggableHandle(
                                    onDragStopped = { commitReorderIfNeeded() },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionHeader(title: String) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    @Composable
    private fun DrawOverlayRow(overlay: OverlayQueueEntry, showReturnButton: Boolean) {
        val subtitle = overlay.item.sourceLabel?.takeIf { it.isNotBlank() }
            ?: stringResource(Res.string.queue_overlay_external_fallback)
        val containerColor = if (overlay.isPlaying) {
            nowPlayingHighlightColor()
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = component::onOverlayRowClicked),
                colors = CardColors(
                    containerColor = containerColor,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AudioFile,
                        contentDescription = null,
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = overlay.item.title)
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (showReturnButton) {
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = component::onOverlayReturnToQueueClicked,
                ) {
                    Text(stringResource(Res.string.queue_overlay_return_to_queue))
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DrawQueueRow(
        queuePositionOneBased: Int,
        entry: NowPlayingQueueEntry,
        overlayActive: Boolean,
        isDragging: Boolean,
        canMoveUp: Boolean,
        canMoveDown: Boolean,
        menuDragModifier: Modifier,
    ) {
        val isNowPlaying = entry.isPlaying && !overlayActive
        val rowAlpha = if (overlayActive) 0.7f else 1f
        CardWithPopup(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(rowAlpha)
                .zIndex(if (isDragging) 1f else 0f),
            title = formatQueueRowTitle(queuePositionOneBased = queuePositionOneBased, entry = entry),
            onCardClicked = { component.onSongClicked(entry) },
            containerColor = if (isNowPlaying) nowPlayingHighlightColor() else null,
            popupActions = buildList {
                add(
                    PopupAction(stringResource(Res.string.queue_action_open_artist)) {
                        component.onOpenArtistClicked(entry)
                    }
                )
                add(
                    PopupAction(stringResource(Res.string.queue_action_open_album)) {
                        component.onOpenAlbumClicked(entry)
                    }
                )
                add(
                    PopupAction(stringResource(Res.string.action_add_to_playlist)) {
                        component.onAddToPlaylistClicked(
                            SongInQueueItem(
                                id = entry.id,
                                song = entry.song,
                                isPlaying = entry.isPlaying,
                            )
                        )
                    }
                )
                if (canMoveUp) {
                    add(
                        PopupAction(stringResource(Res.string.queue_move_up)) {
                            component.onMoveTrackUp(entry.id)
                        }
                    )
                    add(
                        PopupAction(stringResource(Res.string.queue_move_to_top)) {
                            component.onMoveTrackToTop(entry.id)
                        }
                    )
                }
                if (canMoveDown) {
                    add(
                        PopupAction(stringResource(Res.string.queue_move_down)) {
                            component.onMoveTrackDown(entry.id)
                        }
                    )
                    add(
                        PopupAction(stringResource(Res.string.queue_move_to_bottom)) {
                            component.onMoveTrackToBottom(entry.id)
                        }
                    )
                }
                add(
                    PopupAction(stringResource(Res.string.queue_remove_track)) {
                        component.onRemoveTrack(entry)
                    }
                )
            },
            descriptions = buildList {
                add(stringResource(Res.string.queue_track_meta, entry.song.artist, entry.song.album))
                entry.interruptedPositionMs?.takeIf { overlayActive }?.let { positionMs ->
                    add(
                        stringResource(
                            Res.string.queue_interrupted_at,
                            formatInterruptedTime(positionMs),
                        )
                    )
                }
            },
            leadingContent = {
                CoverThumbnail(model = albumArtProvider.albumArtUri(entry.song.albumId))
            },
            menuModifier = menuDragModifier,
        )
    }

    @Composable
    private fun SavePlaylistDialog(
        initialName: String,
        nameError: Boolean,
        onDismiss: () -> Unit,
        onConfirm: (String) -> Unit,
    ) {
        var value by remember(initialName) { mutableStateOf(initialName) }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.queue_save_playlist_dialog_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        singleLine = true,
                        isError = nameError,
                    )
                    if (nameError) {
                        Text(
                            text = stringResource(Res.string.queue_name_taken),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(value) }) {
                    Text(stringResource(Res.string.queue_save_playlist))
                }
            },
            dismissButton = {},
        )
    }
}

@Composable
private fun nowPlayingHighlightColor(): Color =
    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f)

private fun queueLazyOffset(model: NowPlayingScreenModel): Int {
    if (model.overlay == null) return 0
    var offset = 2
    if (model.queue.isNotEmpty()) offset += 1
    return offset
}

private fun lazyListIndexForQueueItem(queueIndex: Int, model: NowPlayingScreenModel): Int =
    queueLazyOffset(model) + queueIndex

private suspend fun LazyListState.awaitReadyForScroll(targetIndex: Int) {
    snapshotFlow {
        val info = layoutInfo
        val hasViewport = info.viewportEndOffset > info.viewportStartOffset
        info.totalItemsCount > targetIndex && hasViewport
    }.first { it }
    withFrameMillis { }
}

private fun formatQueueRowTitle(queuePositionOneBased: Int, entry: NowPlayingQueueEntry): String {
    val trackInAlbum = entry.song.index.trim()
    val albumTrackPart = if (trackInAlbum.isNotEmpty()) "($trackInAlbum) - " else ""
    return "$queuePositionOneBased. - $albumTrackPart${entry.song.name}"
}

internal fun formatInterruptedTime(timeMs: Long): String {
    val seconds = abs(timeMs / 1000)
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
