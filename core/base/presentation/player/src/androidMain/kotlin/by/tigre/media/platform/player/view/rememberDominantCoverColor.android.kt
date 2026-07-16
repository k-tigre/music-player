package by.tigre.media.platform.player.view

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import by.tigre.logger.Log
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
                .size(96)
                .build(),
        )
        val bitmap = when (val image = result.image) {
            null -> null
            is BitmapDrawable -> image.bitmap
            else -> image.toBitmap()
        } ?: return null

        val tint = sampleChromaticTint(bitmap)
        Log.d("CoverTint") {
            tint?.let { "tint=#${it.toArgbHex()} model=$model" } ?: "tint=null model=$model"
        }
        tint
    } catch (_: Exception) {
        null
    }
}

/**
 * Hue-histogram of saturated mid-value pixels. Skips highlights (ice/lightning)
 * and near-greys so album blues/reds win over yellow speculars.
 */
private fun sampleChromaticTint(bitmap: Bitmap): Color? {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0) return null

    val step = max(1, min(w, h) / 48)
    val bins = 36
    val weight = FloatArray(bins)
    val rAcc = FloatArray(bins)
    val gAcc = FloatArray(bins)
    val bAcc = FloatArray(bins)
    val hsv = FloatArray(3)

    var y = 0
    while (y < h) {
        var x = 0
        while (x < w) {
            val px = bitmap.getPixel(x, y)
            if ((px ushr 24) and 0xFF < 200) {
                x += step
                continue
            }
            AndroidColor.colorToHSV(px, hsv)
            val sat = hsv[1]
            val value = hsv[2]
            // Drop greys and bright highlights (yellow ice / lightning).
            if (sat >= 0.28f && value in 0.18f..0.72f) {
                val bin = ((hsv[0] / 360f) * bins).toInt().coerceIn(0, bins - 1)
                // Favor richer mid tones.
                val wgt = sat * sat * (1f - abs(value - 0.42f) * 1.4f).coerceAtLeast(0.15f)
                weight[bin] += wgt
                rAcc[bin] += AndroidColor.red(px) / 255f * wgt
                gAcc[bin] += AndroidColor.green(px) / 255f * wgt
                bAcc[bin] += AndroidColor.blue(px) / 255f * wgt
            }
            x += step
        }
        y += step
    }

    val best = weight.indices.maxByOrNull { weight[it] } ?: return null
    if (weight[best] < 0.01f) return null
    val inv = 1f / weight[best]
    return Color(
        red = (rAcc[best] * inv).coerceIn(0f, 1f),
        green = (gAcc[best] * inv).coerceIn(0f, 1f),
        blue = (bAcc[best] * inv).coerceIn(0f, 1f),
        alpha = 1f,
    )
}

private fun Color.toArgbHex(): String {
    val a = (alpha * 255).toInt().coerceIn(0, 255)
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return "%02X%02X%02X%02X".format(a, r, g, b)
}
