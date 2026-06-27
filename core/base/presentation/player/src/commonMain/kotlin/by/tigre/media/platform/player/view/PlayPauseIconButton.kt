package by.tigre.media.platform.player.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PlayPauseIconButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 56.dp,
    contentDescription: String? = null,
    tint: Color = Color.Unspecified,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        val iconTint = if (tint == Color.Unspecified) LocalContentColor.current else tint
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (scaleIn(initialScale = 0.85f, animationSpec = tween(150)) + fadeIn(tween(150)))
                    .togetherWith(scaleOut(targetScale = 0.85f, animationSpec = tween(150)) + fadeOut(tween(150)))
            },
            label = "playPause",
        ) { playing ->
            Icon(
                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = iconTint,
            )
        }
    }
}

@Composable
fun AnimatedPlayPauseIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 40.dp,
    contentDescription: String? = null,
    tint: Color = Color.Unspecified,
) {
    val iconTint = if (tint == Color.Unspecified) LocalContentColor.current else tint
    AnimatedContent(
        targetState = isPlaying,
        modifier = modifier,
        transitionSpec = {
            (scaleIn(initialScale = 0.85f, animationSpec = tween(150)) + fadeIn(tween(150)))
                .togetherWith(scaleOut(targetScale = 0.85f, animationSpec = tween(150)) + fadeOut(tween(150)))
        },
        label = "playPauseIcon",
    ) { playing ->
        Icon(
            imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = iconTint,
        )
    }
}
