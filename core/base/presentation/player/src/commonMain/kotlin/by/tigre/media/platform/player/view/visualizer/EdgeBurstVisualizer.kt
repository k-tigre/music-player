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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Straight-edge ambilight visualizer: ticks along top/bottom/left/right of a centered square.
 * Optional [edgeTaperPower] shortens corners / peaks mid-edge (sunburst style).
 */
@Composable
fun EdgeBurstVisualizer(
    amplitudes: FloatArray,
    artwork: ImageBitmap,
    modifier: Modifier = Modifier,
    artInset: Float = 0.74f,
    barThicknessFraction: Float = 0.005f,
    barSpacingFraction: Float = 0.0025f,
    minBarLengthFraction: Float = 0.015f,
    sampleRadius: Int = 2,
    brightenWithAmplitude: Boolean = true,
    /** null = uniform length; e.g. 3f = sunburst taper toward corners. */
    edgeTaperPower: Float? = null,
    cornerFloor: Float = 0f,
    barCap: StrokeCap = StrokeCap.Round,
    content: @Composable () -> Unit,
) {
    val pixelMap = remember(artwork) { artwork.toPixelMap() }

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize(),
            onDraw = {
                drawEdgeBars(
                    amplitudes = amplitudes,
                    pixelMap = pixelMap,
                    artInset = artInset,
                    barThicknessFraction = barThicknessFraction,
                    barSpacingFraction = barSpacingFraction,
                    minBarLengthFraction = minBarLengthFraction,
                    sampleRadius = sampleRadius,
                    brightenWithAmplitude = brightenWithAmplitude,
                    edgeTaperPower = edgeTaperPower,
                    cornerFloor = cornerFloor,
                    barCap = barCap,
                )
            },
        )
        content()
    }
}

private fun DrawScope.drawEdgeBars(
    amplitudes: FloatArray,
    pixelMap: PixelMap,
    artInset: Float,
    barThicknessFraction: Float,
    barSpacingFraction: Float,
    minBarLengthFraction: Float,
    sampleRadius: Int,
    brightenWithAmplitude: Boolean,
    edgeTaperPower: Float?,
    cornerFloor: Float,
    barCap: StrokeCap,
) {
    val ccx = size.width / 2f
    val ccy = size.height / 2f
    val box = min(size.width, size.height)
    val artSize = box * artInset
    val half = artSize / 2f
    val barThickness = (box * barThicknessFraction).coerceIn(1.5f, 5f)
    val barSpacing = (box * barSpacingFraction).coerceIn(0.4f, 3f)
    val step = barThickness + barSpacing
    // Mid-edge peak reaches the outer box edge at full amplitude.
    val maxBarLength = ((box - artSize) / 2f).coerceAtLeast(box * 0.04f)
    val minBarLength = (box * minBarLengthFraction).coerceAtMost(maxBarLength * 0.25f)

    val perEdgeCount = max(1, (artSize / step).toInt())
    val totalBars = perEdgeCount * 4
    val pw = pixelMap.width
    val ph = pixelMap.height

    fun ampAt(globalIndex: Int): Float {
        if (amplitudes.isEmpty()) return 0f
        val idx = (globalIndex * amplitudes.size / totalBars).coerceIn(0, amplitudes.size - 1)
        return amplitudes[idx].coerceIn(0f, 1f)
    }

    fun barLength(u: Float, amp: Float): Float {
        val base = minBarLength + amp * (maxBarLength - minBarLength)
        val power = edgeTaperPower ?: return base
        val taper = sin(PI.toFloat() * u).coerceIn(0f, 1f).pow(power)
        return cornerFloor + (base - cornerFloor).coerceAtLeast(0f) * taper
    }

    fun barColor(base: Color, amp: Float): Color =
        if (brightenWithAmplitude) lightenEdge(base, amp * 0.3f) else base

    fun drawTick(color: Color, start: Offset, end: Offset) {
        if (barCap == StrokeCap.Round) {
            drawLine(color.copy(alpha = color.alpha * 0.35f), start, end, barThickness * 3.2f, StrokeCap.Round)
        }
        drawLine(color, start, end, barThickness, barCap)
    }

    var gi = 0

    for (i in 0 until perEdgeCount) {
        val u = (i + 0.5f) / perEdgeCount
        val x = ccx - half + u * artSize
        val amp = ampAt(gi++)
        val len = barLength(u, amp)
        val px = (u * (pw - 1)).toInt().coerceIn(0, pw - 1)
        val color = barColor(averageEdgeColor(pixelMap, px, 0, sampleRadius), amp)
        drawTick(color, Offset(x, ccy - half), Offset(x, ccy - half - len))
    }

    for (i in 0 until perEdgeCount) {
        val v = (i + 0.5f) / perEdgeCount
        val y = ccy - half + v * artSize
        val amp = ampAt(gi++)
        val len = barLength(v, amp)
        val py = (v * (ph - 1)).toInt().coerceIn(0, ph - 1)
        val color = barColor(averageEdgeColor(pixelMap, pw - 1, py, sampleRadius), amp)
        drawTick(color, Offset(ccx + half, y), Offset(ccx + half + len, y))
    }

    for (i in 0 until perEdgeCount) {
        val u = (i + 0.5f) / perEdgeCount
        val x = ccx + half - u * artSize
        val amp = ampAt(gi++)
        val len = barLength(u, amp)
        val px = ((1f - u) * (pw - 1)).toInt().coerceIn(0, pw - 1)
        val color = barColor(averageEdgeColor(pixelMap, px, ph - 1, sampleRadius), amp)
        drawTick(color, Offset(x, ccy + half), Offset(x, ccy + half + len))
    }

    for (i in 0 until perEdgeCount) {
        val v = (i + 0.5f) / perEdgeCount
        val y = ccy + half - v * artSize
        val amp = ampAt(gi++)
        val len = barLength(v, amp)
        val py = ((1f - v) * (ph - 1)).toInt().coerceIn(0, ph - 1)
        val color = barColor(averageEdgeColor(pixelMap, 0, py, sampleRadius), amp)
        drawTick(color, Offset(ccx - half, y), Offset(ccx - half - len, y))
    }
}

private fun averageEdgeColor(pixelMap: PixelMap, cx: Int, cy: Int, radius: Int): Color {
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

private fun lightenEdge(color: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = color.red + (1f - color.red) * t,
        green = color.green + (1f - color.green) * t,
        blue = color.blue + (1f - color.blue) * t,
        alpha = color.alpha,
    )
}
