package by.tigre.media.platform.player.view.visualizer

import androidx.compose.ui.graphics.Color
import by.tigre.media.platform.playback.SpectrumFrame
import by.tigre.media.platform.playback.VisualizerCoverLayout
import by.tigre.media.platform.playback.VisualizerMode
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max

data class VisualizerColors(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
)

/**
 * Lift value/saturation in HSV so cover tints stay readable on a dark blurred backdrop
 * without washing out to white (hue preserved).
 */
fun Color.boostForDarkBackdrop(
    minValue: Float = 0.68f,
    tipValue: Float = 0.88f,
    minSaturation: Float = 0.45f,
): Pair</*base*/ Color, /*tip*/ Color> {
    val hsv = rgbToHsv(red, green, blue)
    val h = hsv[0]
    val s = hsv[1]
    val v = hsv[2]
    val sat = max(s, minSaturation).coerceAtMost(1f)
    val base = hsvToColor(h, sat, max(v, minValue).coerceAtMost(0.82f))
    val tip = hsvToColor(h, (sat * 0.85f).coerceAtLeast(0.35f), tipValue.coerceIn(0.75f, 0.95f))
    return base to tip
}

private fun rgbToHsv(r: Float, g: Float, b: Float): FloatArray {
    val maxC = max(r, max(g, b))
    val minC = minOf(r, g, b)
    val delta = maxC - minC
    val h = when {
        delta < 1e-6f -> 0f
        maxC == r -> ((g - b) / delta).mod(6f) * 60f
        maxC == g -> ((b - r) / delta + 2f) * 60f
        else -> ((r - g) / delta + 4f) * 60f
    }.let { if (it < 0f) it + 360f else it }
    val s = if (maxC < 1e-6f) 0f else delta / maxC
    return floatArrayOf(h, s, maxC)
}

private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1f - abs((h / 60f).mod(2f) - 1f))
    val m = v - c
    val (rp, gp, bp) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(rp + m, gp + m, bp + m, 1f)
}

private fun Float.mod(m: Float): Float {
    val r = this % m
    return if (r < 0f) r + m else r
}

/** Log frequency axis aligned with AndroidAudioSpectrumSource (50 Hz … 14 kHz). */
object SpectrumScale {
    const val MinHz = 50f
    const val MaxHz = 14_000f

    /** Guide marks without labels. */
    val MarkerHz = floatArrayOf(100f, 250f, 500f, 1_000f, 2_000f, 4_000f, 8_000f)

    fun fractionForHz(hz: Float): Float {
        val t = ln((hz / MinHz).toDouble()) / ln((MaxHz / MinHz).toDouble())
        return t.toFloat().coerceIn(0f, 1f)
    }
}

fun VisualizerMode.coverLayout(): VisualizerCoverLayout = when (this) {
    VisualizerMode.Off,
    VisualizerMode.AuraRingCircle,
    VisualizerMode.AuraRingCenter,
    VisualizerMode.RadialBarsInward,
    VisualizerMode.SquircleBurst,
    VisualizerMode.EdgeBurst,
    VisualizerMode.EdgeBurstTaper,
    VisualizerMode.EdgeBurstButt,
    VisualizerMode.CoverPulse,
    VisualizerMode.BeatFlash,
    VisualizerMode.WaveFloor,
    -> VisualizerCoverLayout.Large
    VisualizerMode.RadialBars,
    VisualizerMode.RadialBarsOutward,
    VisualizerMode.SpectrumRibbon,
    VisualizerMode.LiquidBlob,
    VisualizerMode.Particles,
    -> VisualizerCoverLayout.Surrounded
}

/** Modes drawn on top of the (full-size) cover. */
fun VisualizerMode.isCoverOverlay(): Boolean = when (this) {
    VisualizerMode.AuraRingCircle,
    VisualizerMode.AuraRingCenter,
    VisualizerMode.RadialBarsInward,
    VisualizerMode.BeatFlash,
    -> true
    else -> false
}

/** Circular clip on the cover art. */
fun VisualizerMode.clipsCoverToCircle(): Boolean = when (this) {
    VisualizerMode.AuraRingCircle,
    VisualizerMode.RadialBarsInward,
    VisualizerMode.RadialBarsOutward,
    -> true
    else -> false
}

/**
 * Cover size relative to the host box.
 * [RadialBarsOutward] keeps a larger circle so bars have a thinner outer ring.
 */
fun VisualizerMode.coverScale(): Float = when (coverLayout()) {
    VisualizerCoverLayout.Surrounded -> when (this) {
        VisualizerMode.RadialBarsOutward -> 0.84f
        else -> 0.62f
    }
    VisualizerCoverLayout.Large, VisualizerCoverLayout.Minimal -> 1f
}

/** Modes drawn behind a shrunken cover. */
fun VisualizerMode.isSurroundedUnderlay(): Boolean = when (this) {
    VisualizerMode.RadialBars,
    VisualizerMode.RadialBarsOutward,
    VisualizerMode.SpectrumRibbon,
    VisualizerMode.LiquidBlob,
    VisualizerMode.Particles,
    -> true
    else -> false
}

fun emptySpectrumFrame(): SpectrumFrame = SpectrumFrame(
    bands = FloatArray(48),
    rms = 0f,
    beatPulse = 0f,
    timestampMs = 0L,
)
