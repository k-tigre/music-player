package by.tigre.music.player.desktop.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.presentation.catalog.component.BasePlaybackController
import by.tigre.music.player.core.presentation.catalog.component.PlayerItem
import by.tigre.music.player.core.presentation.catalog.view.PlayerProgressSlider
import by.tigre.music.player.desktop.presentation.theme.DesktopBorder
import by.tigre.music.player.desktop.presentation.theme.DesktopGreen
import by.tigre.music.player.desktop.presentation.theme.DesktopPanel
import by.tigre.music.player.desktop.presentation.theme.DesktopSubText
import by.tigre.music.player.desktop.presentation.theme.DesktopText
import by.tigre.music.player.desktop.presentation.theme.DesktopTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.GraphicsEnvironment

private val OVERLAY_WIDTH = 340.dp
private val OVERLAY_HEIGHT = 130.dp
private const val AUTO_DISMISS_MS = 6_000L

@Composable
fun NotificationOverlayWindow(
    controller: BasePlaybackController,
    notificationItem: PlayerItem?,
    iconPainter: Painter,
    showId: Int,
    onDismiss: () -> Unit,
) {
    val screenBounds = remember {
        GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    }
    val windowState = rememberWindowState(
        width = OVERLAY_WIDTH,
        height = OVERLAY_HEIGHT,
        position = WindowPosition.Absolute(
            x = (screenBounds.x + screenBounds.width - OVERLAY_WIDTH.value.toInt() - 16).dp,
            y = (screenBounds.y + screenBounds.height - OVERLAY_HEIGHT.value.toInt() - 16).dp,
        )
    )

    Window(
        onCloseRequest = onDismiss,
        state = windowState,
        undecorated = true,
        transparent = true,
        resizable = false,
        alwaysOnTop = true,
        title = "",
    ) {
        LaunchedEffect(showId) {
            delay(AUTO_DISMISS_MS)
            onDismiss()
        }

        val playerState by controller.player.state.collectAsState()
        val progress by controller.player.progress.collectAsState(initial = PlaybackPlayer.Progress(0L, 0L))
        val scope = rememberCoroutineScope()

        val fraction = if (progress.duration > 0) progress.position.toFloat() / progress.duration else 0f

        DesktopTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DesktopPanel)
                    .border(1.dp, DesktopBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // ── Close button ──────────────────────────────────────────────────
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = DesktopSubText,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // ── Track info ────────────────────────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 20.dp)
                    ) {
                        Icon(
                            painter = iconPainter,
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notificationItem?.title ?: "",
                                color = DesktopText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = notificationItem?.let { item ->
                                    item.artist
                                        ?.let { a -> item.album?.let { "$a • $it" } ?: a }
                                        ?: item.album
                                        ?: item.subtitle
                                } ?: "",
                                color = DesktopSubText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    // ── Progress slider ───────────────────────────────────────────
                    PlayerProgressSlider(
                        fraction = fraction,
                        onSeekTo = { f ->
                            scope.launch {
                                controller.player.seekTo((f * progress.duration).toLong())
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── Transport controls ────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-10).dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = controller::playPrev,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = null,
                                tint = DesktopText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DesktopGreen)
                                .clickable {
                                    if (playerState == PlaybackPlayer.State.Playing) controller.pause()
                                    else controller.resume()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playerState == PlaybackPlayer.State.Playing)
                                    Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = controller::playNext,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null,
                                tint = DesktopText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
