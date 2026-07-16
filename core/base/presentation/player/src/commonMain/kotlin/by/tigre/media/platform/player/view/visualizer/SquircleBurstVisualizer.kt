package by.tigre.media.platform.player.view.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toPixelMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Radial squircle visualizer — bars radiate from a rounded-square base.
 * Bar colors are sampled from [artwork] along each bar's angle (cached per artwork).
 */
@Composable
fun SquircleBurstVisualizer(
    amplitudes: FloatArray,
    artwork: ImageBitmap,
    modifier: Modifier = Modifier,
    barWidthFraction: Float = 0.008f,
    minBarLengthFraction: Float = 0.015f,
    maxBarLengthFraction: Float = 0.20f,
    squircleExponent: Float = 4f,
    baseInset: Float = 0.62f,
    sampleRadius: Int = 2,
    brightenWithAmplitude: Boolean = true,
    content: @Composable () -> Unit,
) {
    val barCount = amplitudes.size.coerceAtLeast(1)

    val barColors = remember(artwork, barCount, squircleExponent, sampleRadius) {
        sampleBarColors(artwork, barCount, squircleExponent, sampleRadius)
    }

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize(),
            onDraw = {
                drawSquircleBars(
                    amplitudes = amplitudes,
                    barColors = barColors,
                    barCount = barCount,
                    squircleExponent = squircleExponent,
                    baseInset = baseInset,
                    barWidthFraction = barWidthFraction,
                    minBarLengthFraction = minBarLengthFraction,
                    maxBarLengthFraction = maxBarLengthFraction,
                    brightenWithAmplitude = brightenWithAmplitude,
                )
            },
        )
        content()
    }
}

private fun sampleBarColors(
    artwork: ImageBitmap,
    barCount: Int,
    squircleExponent: Float,
    sampleRadius: Int,
): List<Color> {
    val pixelMap = artwork.toPixelMap()
    val w = pixelMap.width
    val h = pixelMap.height
    val cx = w / 2f
    val cy = h / 2f
    val half = min(w, h) / 2f
    return List(barCount) { i ->
        val theta = (i.toFloat() / barCount) * 2f * PI.toFloat()
        val ct = cos(theta)
        val st = sin(theta)
        val r = squircleRadius(half, ct, st, squircleExponent) * 0.94f
        val px = (cx + ct * r).toInt().coerceIn(0, w - 1)
        val py = (cy + st * r).toInt().coerceIn(0, h - 1)
        averageColor(pixelMap, px, py, sampleRadius)
    }
}

private fun DrawScope.drawSquircleBars(
    amplitudes: FloatArray,
    barColors: List<Color>,
    barCount: Int,
    squircleExponent: Float,
    baseInset: Float,
    barWidthFraction: Float,
    minBarLengthFraction: Float,
    maxBarLengthFraction: Float,
    brightenWithAmplitude: Boolean,
) {
    val ccx = size.width / 2f
    val ccy = size.height / 2f
    val half = min(size.width, size.height) / 2f
    val baseR = half * baseInset
    val barWidth = (half * 2f * barWidthFraction).coerceIn(2f, 8f)
    val minBarLength = half * 2f * minBarLengthFraction
    val maxBarLength = half * 2f * maxBarLengthFraction

    for (i in 0 until barCount) {
        val theta = (i.toFloat() / barCount) * 2f * PI.toFloat()
        val ct = cos(theta)
        val st = sin(theta)
        val r = squircleRadius(baseR, ct, st, squircleExponent)
        val amp = amplitudes.getOrElse(i) { 0f }.coerceIn(0f, 1f)
        val barLen = minBarLength + amp * (maxBarLength - minBarLength)
        val start = Offset(ccx + ct * r, ccy + st * r)
        val end = Offset(ccx + ct * (r + barLen), ccy + st * (r + barLen))
        val color = if (brightenWithAmplitude) {
            lighten(barColors[i], amp * 0.3f)
        } else {
            barColors[i]
        }
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = barWidth,
            cap = StrokeCap.Round,
        )
    }
}

private fun squircleRadius(base: Float, ct: Float, st: Float, exponent: Float): Float {
    val denom = (
        abs(ct).toDouble().pow(exponent.toDouble()) +
            abs(st).toDouble().pow(exponent.toDouble())
        ).pow(1.0 / exponent).toFloat()
    return base / denom.coerceAtLeast(0.0001f)
}

private fun averageColor(pixelMap: PixelMap, cx: Int, cy: Int, radius: Int): Color {
    var r = 0f
    var g = 0f
    var b = 0f
    var count = 0
    val minX = (cx - radius).coerceAtLeast(0)
    val maxX = (cx + radius).coerceAtMost(pixelMap.width - 1)
    val minY = (cy - radius).coerceAtLeast(0)
    val maxY = (cy + radius).coerceAtMost(pixelMap.height - 1)
    for (x in minX..maxX) {
        for (y in minY..maxY) {
            val c = pixelMap[x, y]
            r += c.red
            g += c.green
            b += c.blue
            count++
        }
    }
    return if (count == 0) Color.Gray else Color(r / count, g / count, b / count, 1f)
}

private fun lighten(color: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = color.red + (1f - color.red) * t,
        green = color.green + (1f - color.green) * t,
        blue = color.blue + (1f - color.blue) * t,
        alpha = color.alpha,
    )
}
