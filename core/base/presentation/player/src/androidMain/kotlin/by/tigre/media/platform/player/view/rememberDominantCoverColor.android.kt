package by.tigre.media.platform.player.view

import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap

@Composable
actual fun rememberDominantCoverColor(coverModel: Any?): Color? {
    val context = LocalContext.current
    var color by remember(coverModel) { mutableStateOf<Color?>(null) }

    LaunchedEffect(coverModel) {
        color = if (coverModel == null) {
            null
        } else {
            extractDominantColor(context, coverModel)
        }
    }

    return color
}

private suspend fun extractDominantColor(context: android.content.Context, model: Any): Color? {
    return try {
        val loader = ImageLoader(context)
        val result = loader.execute(
            ImageRequest.Builder(context)
                .data(model)
                .allowHardware(false)
                .build(),
        )
        val bitmap = when (val image = result.image) {
            null -> null
            is BitmapDrawable -> image.bitmap
            else -> image.toBitmap()
        } ?: return null

        val palette = Palette.from(bitmap).generate()
        val swatch = palette.vibrantSwatch
            ?: palette.dominantSwatch
            ?: palette.mutedSwatch
            ?: return null
        Color(swatch.rgb)
    } catch (_: Exception) {
        null
    }
}
