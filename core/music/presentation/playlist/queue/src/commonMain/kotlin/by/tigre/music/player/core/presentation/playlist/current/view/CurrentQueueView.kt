package by.tigre.music.player.core.presentation.playlist.current.view

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.entiry.playback.NowPlayingQueueEntry
import by.tigre.music.player.core.entiry.playback.NowPlayingScreenModel
import by.tigre.music.player.core.entiry.playback.OverlayQueueEntry
import by.tigre.music.player.core.presentation.playlist.current.component.CurrentQueueComponent
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.media.platform.tools.platform.compose.view.CardWithPopup
import by.tigre.media.platform.tools.platform.compose.view.bottomBarListContentPadding
import by.tigre.media.platform.tools.platform.compose.view.EmptyScreen
import by.tigre.media.platform.tools.platform.compose.view.ErrorScreen
import by.tigre.media.platform.tools.platform.compose.view.PopupAction
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicator
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicatorSize
import `by`.tigre.music.player.core.presentation.queue.resources.Res
import `by`.tigre.music.player.core.presentation.queue.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

class CurrentQueueView(
    private val component: CurrentQueueComponent,
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
            LaunchedEffect(model.overlay?.item?.uri) {
                if (model.overlay != null) {
                    listState.scrollToItem(0)
                }
            }
            LazyColumn(
                state = listState,
                contentPadding = bottomBarListContentPadding(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
        val rowModifier = if (overlay.isPlaying) {
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.medium)
        } else {
            Modifier.fillMaxWidth()
        }
        Column(modifier = rowModifier) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = component::onOverlayRowClicked),
                colors = CardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
        val rowAlpha = if (overlayActive) 0.7f else 1f
        val rowModifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .then(
                if (entry.isPlaying && !overlayActive) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.medium)
                } else {
                    Modifier
                }
            )
        CardWithPopup(
            modifier = rowModifier,
            title = formatQueueRowTitle(queuePositionOneBased = queuePositionOneBased, entry = entry),
            onCardClicked = { component.onSongClicked(entry) },
            popupActions = listOf(
                PopupAction(stringResource(Res.string.queue_action_open_artist)) {
                    component.onOpenArtistClicked(entry)
                },
                PopupAction(stringResource(Res.string.queue_action_open_album)) {
                    component.onOpenAlbumClicked(entry)
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
        )
    }
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
