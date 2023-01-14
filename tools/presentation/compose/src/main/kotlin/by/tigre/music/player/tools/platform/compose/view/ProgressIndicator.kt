package by.tigre.music.player.tools.platform.compose.view

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import by.tigre.music.player.tools.platform.compose.AppMaterial
import by.tigre.music.player.tools.platform.compose.view.ProgressIndicatorSize.LARGE

@Composable
fun BoxScope.DrawProgress(progressVisible: State<Boolean>, modifier: Modifier = Modifier) {
    Crossfade(
        modifier = modifier.align(Alignment.Center),
        targetState = progressVisible.value,
        animationSpec = tween(2000)
    ) { visible ->
        if (visible) {
            ProgressIndicator(
                modifier = modifier,
                size = LARGE
            )
        }
    }
}

@Composable
fun ProgressIndicator(
    modifier: Modifier = Modifier,
    size: ProgressIndicatorSize,
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
    progress: Color = MaterialTheme.colorScheme.primary,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(size.size),
            progress = 1f,
            color = backgroundColor,
            strokeWidth = size.strokeWidth * 2
        )/*background*/
        CircularProgressIndicator(
            modifier = Modifier.size(size.size - size.strokeWidth),
            strokeWidth = size.strokeWidth,
            progress = 0.5f,
            color = progress
        )/*foreground*/
    }
}

enum class ProgressIndicatorSize(val size: Dp, val strokeWidth: Dp) {
    LARGE(60.dp, 4.dp),
    SMALL(28.dp, 2.dp)
}

@Composable
@Preview(backgroundColor = 0xFFFFFF, showBackground = true, widthDp = 100, heightDp = 100)
private fun ProgressIndicatorPreview() {
    AppMaterial.AppTheme {
        ProgressIndicator(
            size = LARGE,
            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            progress = MaterialTheme.colorScheme.primary
        )
    }
}
