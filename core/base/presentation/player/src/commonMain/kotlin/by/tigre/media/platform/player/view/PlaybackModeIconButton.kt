package by.tigre.media.platform.player.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PlaybackModeIconButton(
    onClick: () -> Unit,
    active: Boolean,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    containerSize: Dp = 40.dp,
    activeIconTint: Color = MaterialTheme.colorScheme.primary,
    activeBackground: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
    inactiveIconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minWidth = containerSize, minHeight = containerSize),
    ) {
        Box(
            modifier = Modifier
                .size(containerSize)
                .clip(CircleShape)
                .then(
                    if (active) Modifier.background(activeBackground)
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = if (active) activeIconTint else inactiveIconTint,
            )
        }
    }
}
