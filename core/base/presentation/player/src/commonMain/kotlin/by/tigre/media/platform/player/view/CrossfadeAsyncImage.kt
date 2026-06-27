package by.tigre.media.platform.player.view

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

private const val CoverCrossfadeMillis = 400

@Composable
fun CrossfadeAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Painter? = null,
) {
    val coverKey = model?.toString()

    Crossfade(
        targetState = coverKey,
        modifier = modifier,
        animationSpec = tween(CoverCrossfadeMillis),
        label = "playerCoverCrossfade",
    ) { key ->
        AsyncImage(
            model = key,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            placeholder = placeholder,
            error = placeholder,
            fallback = placeholder,
        )
    }
}
