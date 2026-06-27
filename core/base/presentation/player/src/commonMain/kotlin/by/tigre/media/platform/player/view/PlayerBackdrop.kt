package by.tigre.media.platform.player.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

private const val BackdropColorCrossfadeMillis = 400

@Composable
fun PlayerBackdrop(
    coverModel: Any?,
    modifier: Modifier = Modifier,
    edgeToEdge: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val dominantColor = rememberDominantCoverColor(coverModel)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val targetBaseColor = dominantColor ?: MaterialTheme.colorScheme.primaryContainer
    val baseColor by animateColorAsState(
        targetValue = targetBaseColor,
        animationSpec = tween(BackdropColorCrossfadeMillis),
        label = "playerBackdropBaseColor",
    )
    val hasCover = coverModel != null

    val gradientStops = backdropGradientStops(
        baseColor = baseColor,
        surfaceColor = surfaceColor,
        hasCover = hasCover,
        isDark = isDark,
    )
    val statusBarSampleColor = dominantColor
        ?: gradientStops.first().second

    PlayerBackdropSystemBarEffect(
        statusBarColor = Color.Transparent,
        lightStatusBarIcons = statusBarSampleColor.luminance() > 0.5f,
        enabled = edgeToEdge,
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (hasCover) {
            CrossfadeAsyncImage(
                model = coverModel,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.25f)
                    .blur(40.dp),
                contentScale = ContentScale.Crop,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colorStops = gradientStops)),
        )

        content()
    }
}

private fun backdropGradientStops(
    baseColor: Color,
    surfaceColor: Color,
    hasCover: Boolean,
    isDark: Boolean,
): Array<Pair<Float, Color>> {
    if (!hasCover) {
        return arrayOf(
            0f to lerp(surfaceColor, baseColor, if (isDark) 0.35f else 0.25f),
            1f to surfaceColor,
        )
    }

    // Middle stays clear so the blurred cover reads; scrim only ramps up near controls.
    val topVignette = (if (isDark) Color.Black else Color.White)
        .copy(alpha = if (isDark) 0.10f else 0.06f)

    return arrayOf(
        0f to topVignette,
        0.12f to baseColor.copy(alpha = if (isDark) 0.14f else 0.10f),
        0.55f to Color.Transparent,
        0.72f to surfaceColor.copy(alpha = if (isDark) 0.50f else 0.62f),
        0.88f to surfaceColor.copy(alpha = if (isDark) 0.76f else 0.84f),
        1f to surfaceColor.copy(alpha = if (isDark) 0.92f else 0.97f),
    )
}

private fun Color.luminance(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue
