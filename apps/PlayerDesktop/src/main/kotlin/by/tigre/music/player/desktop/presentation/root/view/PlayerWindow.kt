package by.tigre.music.player.desktop.presentation.root.view

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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import by.tigre.music.player.core.presentation.catalog.component.BasePlayerComponent
import by.tigre.music.player.core.presentation.catalog.view.PlayerProgressSlider
import by.tigre.music.player.desktop.resources.Res
import by.tigre.music.player.desktop.resources.desktop_no_track
import by.tigre.music.player.desktop.resources.desktop_window_title_player
import by.tigre.music.player.desktop.presentation.root.view.component.DesktopTitleBar
import org.jetbrains.compose.resources.stringResource
import by.tigre.music.player.desktop.presentation.theme.DesktopBorder
import by.tigre.music.player.desktop.presentation.theme.DesktopButtonBg
import by.tigre.music.player.desktop.presentation.theme.DesktopCoverBg
import by.tigre.music.player.desktop.presentation.theme.DesktopGreen
import by.tigre.music.player.desktop.presentation.theme.DesktopSubText
import by.tigre.music.player.desktop.presentation.theme.DesktopText
import coil3.compose.AsyncImage

@Composable
internal fun PlayerWindowContent(
    player: BasePlayerComponent,
    libraryVisible: Boolean,
    onToggleLibrary: () -> Unit,
    equalizerVisible: Boolean,
    onToggleEqualizer: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onClose: () -> Unit,
) {
    val current by player.currentItem.collectAsState()
    val state by player.state.collectAsState()
    val fraction by player.fraction.collectAsState()
    val position by player.position.collectAsState()
    val isNormal by player.isNormal.collectAsState()
    val eqAvailable by player.playbackEqualizer.isAvailable.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Custom title bar (draggable) ─────────────────────────────────────
        DesktopTitleBar(
            title = stringResource(Res.string.desktop_window_title_player),
            onDragStart = onDragStart,
            onDrag = onDrag,
            onClose = onClose,
        )

        HorizontalDivider(color = DesktopBorder, thickness = 1.dp)

        // ── Cover + info ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DesktopCoverBg),
                contentAlignment = Alignment.Center
            ) {
                val uri = current?.coverUri
                if (uri != null) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = DesktopGreen.copy(alpha = 0.45f),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = current?.title ?: stringResource(Res.string.desktop_no_track),
                    color = DesktopText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = current?.subtitle ?: "---",
                    color = DesktopSubText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── Seek slider ──────────────────────────────────────────────────────
        PlayerProgressSlider(
            fraction = fraction,
            onSeekTo = player::seekTo,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-10).dp)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = position.current,
                color = DesktopGreen,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = position.left,
                color = DesktopSubText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // ── Transport controls ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { player.switchMode(!isNormal) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isNormal) Icons.Default.Repeat else Icons.Default.Shuffle,
                    contentDescription = null,
                    tint = if (isNormal) DesktopSubText else DesktopGreen,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(4.dp))

            IconButton(
                onClick = player::prev,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = null,
                    tint = DesktopText,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Play / Pause — circular green button
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DesktopGreen)
                    .clickable {
                        if (state == BasePlayerComponent.State.Playing) player.pause()
                        else player.play()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state == BasePlayerComponent.State.Playing)
                        Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(
                onClick = player::next,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = DesktopText,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(4.dp))

            if (eqAvailable) {
                val eqBorder = if (equalizerVisible) DesktopGreen else DesktopBorder
                val eqBg = if (equalizerVisible) DesktopGreen.copy(alpha = 0.15f) else DesktopButtonBg
                val eqColor = if (equalizerVisible) DesktopGreen else DesktopSubText

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(eqBg)
                        .border(1.dp, eqBorder, RoundedCornerShape(2.dp))
                        .clickable(onClick = onToggleEqualizer)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EQ",
                        color = eqColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(Modifier.width(6.dp))
            }

            // [PL] toggle library window
            val plBorder = if (libraryVisible) DesktopGreen else DesktopBorder
            val plBg = if (libraryVisible) DesktopGreen.copy(alpha = 0.15f) else DesktopButtonBg
            val plColor = if (libraryVisible) DesktopGreen else DesktopSubText

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(plBg)
                    .border(1.dp, plBorder, RoundedCornerShape(2.dp))
                    .clickable(onClick = onToggleLibrary)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PL",
                    color = plColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
