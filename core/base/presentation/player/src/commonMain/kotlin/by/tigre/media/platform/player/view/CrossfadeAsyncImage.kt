package by.tigre.media.platform.player.view

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import by.tigre.media.platform.tools.platform.compose.view.logCoverLoadError
import by.tigre.media.platform.tools.platform.compose.view.rememberResolvedCoverModel
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
    val resolved = rememberResolvedCoverModel(model)
    Crossfade(
        targetState = resolved,
        modifier = modifier,
        animationSpec = tween(CoverCrossfadeMillis),
        label = "playerCoverCrossfade",
    ) { current ->
        // Coil 3 throws NullRequestDataException if model is null; show placeholder instead.
        if (current == null) {
            if (placeholder != null) {
                Image(
                    painter = placeholder,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
            }
        } else {
            AsyncImage(
                model = current,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                placeholder = placeholder,
                error = placeholder,
                fallback = placeholder,
                onError = { error ->
                    logCoverLoadError(current, error.result.throwable)
                },
            )
        }
    }
}
