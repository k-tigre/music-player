package by.tigre.media.platform.player.view

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.player.component.BasePlayerComponent
import by.tigre.media.platform.player.component.RepeatMode
import by.tigre.media.platform.player.component.PlayerComponent
import by.tigre.media.platform.player.component.PlayerItem
import by.tigre.media.platform.player.component.SmallPlayerComponent
import by.tigre.media.platform.tools.platform.compose.ComposableView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SmallPlayerView(
    private val component: SmallPlayerComponent,
    private val showOrderModeButton: Boolean = true,
) : ComposableView {

    @Composable
    override fun Draw(modifier: Modifier) {
        val current = component.currentItem.collectAsState().value
        if (current != null) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .offset(y = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = MaterialTheme.shapes.extraLarge.copy(
                                bottomStart = CornerSize(0),
                                bottomEnd = CornerSize(0)
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DrawItem(
                            modifier = Modifier
                                .animateContentSize(tween(250)),
                            item = current
                        )
                        DrawActions(current)
                    }
                }

                DrawProgress()
            }
        }
    }

    @Composable
    private fun DrawItem(modifier: Modifier, item: PlayerItem) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable { component.showPlayerView() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = item.title,
                )

                Text(
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (item.canReturnToQueue) {
                IconButton(onClick = component::returnToQueue) {
                    Icon(
                        contentDescription = "Return to queue",
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                    )
                }
            }
        }
    }

    @Composable
    private fun BoxScope.DrawProgress() {
        val position = component.fraction.collectAsState()

        PlayerProgressSlider(
            fraction = position.value,
            onSeekTo = component::seekTo,
            onSeekCommitted = component::onSeekCommitted,
            modifier = Modifier
                .offset(y = (-22).dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter)
        )
    }

    @Composable
    private fun DrawActions(current: PlayerItem) {
        Row(
            modifier = Modifier.padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val state = component.state.collectAsState()
            val position = component.position.collectAsState().value
            val showOrderMode = showOrderModeButton && !current.isExternal
            val shuffleEnabled = component.shuffleEnabled.collectAsState().value
            val repeatMode = component.repeatMode.collectAsState().value
            val repeatActive = repeatMode != RepeatMode.Off

            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = "${position.current}/${position.total}",
            )

            if (showOrderMode) {
                PlaybackModeIconButton(
                    onClick = component::toggleShuffle,
                    active = shuffleEnabled,
                    imageVector = Icons.Default.Shuffle,
                    iconSize = 22.dp,
                    containerSize = 36.dp,
                )
            }

            Spacer(Modifier.weight(1f))

            IconButton(onClick = component::prev) {
                Icon(contentDescription = null, imageVector = Icons.Default.SkipPrevious)
            }

            if (state.value == BasePlayerComponent.State.Playing) {
                IconButton(onClick = component::pause) {
                    Icon(contentDescription = null, imageVector = Icons.Default.Pause)
                }
            } else {
                IconButton(onClick = component::play) {
                    Icon(contentDescription = null, imageVector = Icons.Default.PlayArrow)
                }
            }

            IconButton(onClick = component::next) {
                Icon(contentDescription = null, imageVector = Icons.Default.SkipNext)
            }

            Spacer(Modifier.weight(1f))

            if (showOrderMode) {
                PlaybackModeIconButton(
                    onClick = component::cycleRepeat,
                    active = repeatActive,
                    imageVector = when (repeatMode) {
                        RepeatMode.One -> Icons.Default.RepeatOne
                        RepeatMode.All, RepeatMode.Off -> Icons.Default.Repeat
                    },
                    iconSize = 22.dp,
                    containerSize = 36.dp,
                )
            }
        }
    }
}
