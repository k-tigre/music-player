package by.tigre.media.platform.player.view

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
    private val config: Config = Config(),
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = MaterialTheme.shapes.extraLarge.copy(
                                bottomStart = CornerSize(0),
                                bottomEnd = CornerSize(0)
                            )
                        )
                        .then(
                            if (config.extendUnderNavigationBar) {
                                Modifier.navigationBarsPadding()
                            } else {
                                Modifier
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
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
        if (config.actionsMode == PlayerView.ActionsMode.SeekButtons) {
            DrawSeekActions()
            return
        }

        Row(
            modifier = Modifier.padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val state = component.state.collectAsState()
            val position = component.position.collectAsState().value
            val showOrderMode = config.showOrderModeButton && !current.isExternal
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

            PlayPauseIconButton(
                isPlaying = state.value == BasePlayerComponent.State.Playing,
                onClick = if (state.value == BasePlayerComponent.State.Playing) {
                    component::pause
                } else {
                    component::play
                },
                iconSize = 24.dp,
            )

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

    @Composable
    private fun DrawSeekActions() {
        val state = component.state.collectAsState()
        val position = component.position.collectAsState().value
        val isPlaying = state.value == BasePlayerComponent.State.Playing

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = "${position.current}/${position.total}",
            )

            Spacer(Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SeekControlSlot(
                    onClick = component::seekBack1Minute,
                    imageVector = Icons.Filled.FastRewind,
                    caption = config.seek1MinuteDurationCaption,
                    contentDescription = config.seekBack1MinuteLabel,
                )
                SeekControlSlot(
                    onClick = component::seekBack15Seconds,
                    imageVector = Icons.Filled.FastRewind,
                    caption = config.seek15SecondsDurationCaption,
                    contentDescription = config.seekBack15SecondsLabel,
                )
                SeekControlSlot(
                    onClick = if (isPlaying) component::pause else component::play,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    caption = null,
                ) {
                    AnimatedPlayPauseIcon(
                        isPlaying = isPlaying,
                        iconSize = SeekControlIconSize,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                SeekControlSlot(
                    onClick = component::seekForward15Seconds,
                    imageVector = Icons.Filled.FastForward,
                    caption = config.seek15SecondsDurationCaption,
                    contentDescription = config.seekForward15SecondsLabel,
                )
                SeekControlSlot(
                    onClick = component::seekForward1Minute,
                    imageVector = Icons.Filled.FastForward,
                    caption = config.seek1MinuteDurationCaption,
                    contentDescription = config.seekForward1MinuteLabel,
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }

    @Composable
    private fun seekCaptionHeight(): Dp {
        val style = MaterialTheme.typography.labelSmall
        return with(LocalDensity.current) {
            style.lineHeight.toDp()
        }
    }

    @Composable
    private fun SeekControlSlot(
        onClick: () -> Unit,
        imageVector: ImageVector,
        contentDescription: String,
        caption: String?,
    ) {
        val color = MaterialTheme.colorScheme.onSurface
        SeekControlSlot(
            onClick = onClick,
            contentDescription = contentDescription,
            caption = caption,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.size(SeekControlIconSize),
                tint = color,
            )
        }
    }

    @Composable
    private fun SeekControlSlot(
        onClick: () -> Unit,
        contentDescription: String,
        caption: String?,
        icon: @Composable () -> Unit,
    ) {
        val color = MaterialTheme.colorScheme.onSurface
        Column(
            modifier = Modifier
                .defaultMinSize(minWidth = SeekControlSlotMinWidth)
                .semantics {
                    role = Role.Button
                    this.contentDescription = contentDescription
                }
                .clickable(onClick = onClick)
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(SeekControlIconAreaSize),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Box(
                modifier = Modifier
                    .height(seekCaptionHeight())
                    .widthIn(min = SeekControlSlotMinWidth),
                contentAlignment = Alignment.Center,
            ) {
                if (caption != null) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    data class Config(
        val showOrderModeButton: Boolean = true,
        val actionsMode: PlayerView.ActionsMode = PlayerView.ActionsMode.ChapterButtons,
        val extendUnderNavigationBar: Boolean = false,
        val seekBack1MinuteLabel: String = "-1m",
        val seekBack15SecondsLabel: String = "-15s",
        val seekForward15SecondsLabel: String = "+15s",
        val seekForward1MinuteLabel: String = "+1m",
        val seek15SecondsDurationCaption: String = "15s",
        val seek1MinuteDurationCaption: String = "1m",
    )

    private companion object {
        private val SeekControlIconAreaSize = 36.dp
        private val SeekControlIconSize = 22.dp
        private val SeekControlSlotMinWidth = 40.dp
    }
}
