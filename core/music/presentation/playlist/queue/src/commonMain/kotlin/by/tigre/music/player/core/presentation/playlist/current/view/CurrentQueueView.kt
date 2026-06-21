package by.tigre.music.player.core.presentation.playlist.current.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.entiry.playback.NowPlayingQueueEntry
import by.tigre.music.player.core.entiry.playback.NowPlayingScreenModel
import by.tigre.music.player.core.entiry.playback.OverlayQueueEntry
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

class CurrentQueueView(
    private val component: CurrentQueueComponent,
    private val albumArtProvider: AlbumArtProvider,
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        Scaffold(
            modifier = modifier,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(Modifier.padding(horizontal = 48.dp)) {
                            Text(
                                text = stringResource(Res.string.screen_current_queue_title),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                )
            },
            content = { paddingValues ->
                val screenState by component.screenState.collectAsState()

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
            LaunchedEffect(model.overlay?.item?.uri) {
                if (model.overlay != null) {
                    listState.scrollToItem(0)
                } else {
                    val playingIndex = model.queue.indexOfFirst { it.isPlaying }
                    if (playingIndex >= 0) {
                        listState.smartScrollToItem(
                            targetIndex = lazyListIndexForQueueItem(playingIndex, model),
                            nearMarginPx = nearMarginPx,
                            instant = true,
                        )
                    }
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
                listState.smartScrollToItem(
                    targetIndex = lazyListIndexForQueueItem(playingIndex, currentModel),
                    nearMarginPx = nearMarginPx,
                    instant = true,
                )
            }
            LaunchedEffect(Unit) {
                component.scrollToPlayingTrackEvents.collect { queueIndex ->
                    listState.smartScrollToItem(
                        targetIndex = lazyListIndexForQueueItem(queueIndex, currentModel),
                        nearMarginPx = nearMarginPx,
                        smoothDurationMillis = 1200,
                    )
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

                if (model.queue.isNotEmpty()) {
                    if (model.overlay != null) {
                        item(key = "section_hold") {
                            SectionHeader(stringResource(Res.string.queue_section_on_hold))
                        }
                    }
                    itemsIndexed(model.queue, key = { _, entry -> entry.id }) { index, entry ->
                        DrawQueueRow(
                            queuePositionOneBased = index + 1,
                            entry = entry,
                            overlayActive = model.overlay != null,
                        )
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
    ) {
        val isNowPlaying = entry.isPlaying && !overlayActive
        val rowAlpha = if (overlayActive) 0.7f else 1f
        CardWithPopup(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(rowAlpha),
            title = formatQueueRowTitle(queuePositionOneBased = queuePositionOneBased, entry = entry),
            onCardClicked = { component.onSongClicked(entry) },
            containerColor = if (isNowPlaying) nowPlayingHighlightColor() else null,
            popupActions = listOf(
                PopupAction(stringResource(Res.string.queue_action_open_artist)) {
                    component.onOpenArtistClicked(entry)
                },
                PopupAction(stringResource(Res.string.queue_action_open_album)) {
                    component.onOpenAlbumClicked(entry)
                },
                PopupAction(stringResource(Res.string.action_add_to_playlist)) {
                    component.onAddToPlaylistClicked(
                        SongInQueueItem(
                            id = entry.id,
                            song = entry.song,
                            isPlaying = entry.isPlaying,
                        )
                    )
                },
            ),
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
        )
    }
}

@Composable
private fun nowPlayingHighlightColor(): Color =
    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f)

private fun lazyListIndexForQueueItem(queueIndex: Int, model: NowPlayingScreenModel): Int {
    var offset = 0
    if (model.overlay != null) offset += 2
    if (model.overlay != null && model.queue.isNotEmpty()) offset += 1
    return offset + queueIndex
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
