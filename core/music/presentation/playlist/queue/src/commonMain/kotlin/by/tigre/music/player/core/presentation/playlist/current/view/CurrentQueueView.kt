package by.tigre.music.player.core.presentation.playlist.current.view

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.entiry.playback.SongInQueueItem
import by.tigre.music.player.core.presentation.playlist.current.component.CurrentQueueComponent
import by.tigre.music.player.presentation.base.ScreenContentState
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
                            DrawContent((screenState as ScreenContentState.Content<List<SongInQueueItem>>).value)
                        }
                    }
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DrawContent(songs: List<SongInQueueItem>) {
        if (songs.isEmpty()) {
            EmptyScreen(
                reloadAction = component::onAddToQueueClicked,
                title = stringResource(Res.string.queue_empty_title),
                message = stringResource(Res.string.queue_empty_message),
                actionTitle = stringResource(Res.string.queue_empty_action)
            )
        } else {
            LazyColumn(
                contentPadding = bottomBarListContentPadding(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                songs.forEachIndexed { index, item ->
                    item {
                        val rowModifier = if (item.isPlaying) {
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.medium)
                        } else {
                            Modifier.fillMaxWidth()
                        }
                        CardWithPopup(
                            modifier = rowModifier,
                            title = formatQueueRowTitle(queuePositionOneBased = index + 1, item = item),
                            onCardClicked = { component.onSongClicked(item) },
                            popupActions = listOf(
                                PopupAction(stringResource(Res.string.queue_action_open_artist)) {
                                    component.onOpenArtistClicked(item)
                                },
                                PopupAction(stringResource(Res.string.queue_action_open_album)) {
                                    component.onOpenAlbumClicked(item)
                                },
                            ),
                            descriptions = listOf(
                                stringResource(Res.string.queue_track_meta, item.song.artist, item.song.album)
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun formatQueueRowTitle(queuePositionOneBased: Int, item: SongInQueueItem): String {
    val trackInAlbum = item.song.index.trim()
    val albumTrackPart = if (trackInAlbum.isNotEmpty()) "($trackInAlbum) - " else ""
    return "$queuePositionOneBased. - $albumTrackPart${item.song.name}"
}
