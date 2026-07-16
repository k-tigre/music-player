package by.tigre.media.platform.player.view.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import by.tigre.media.platform.playback.SpectrumFrame
import by.tigre.media.platform.playback.VisualizerMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun VisualizerCanvas(
    mode: VisualizerMode,
    frame: SpectrumFrame,
    colors: VisualizerColors,
    modifier: Modifier = Modifier,
    /** Fraction of box size occupied by the cover (Surrounded ≈ 0.62). */
    coverFraction: Float = 0.62f,
) {
    when (mode) {
        VisualizerMode.Off -> Unit
        VisualizerMode.AuraRingCircle -> AuraRingCircleCanvas(frame, colors, modifier)
        VisualizerMode.AuraRingCenter -> AuraRingCenterCanvas(frame, colors, modifier)
        VisualizerMode.RadialBars -> RadialBarsCanvas(frame, colors, coverFraction, modifier)
        VisualizerMode.RadialBarsInward -> RadialBarsInwardCanvas(frame, colors, modifier)
        VisualizerMode.RadialBarsOutward -> RadialBarsOutwardCanvas(frame, colors, coverFraction, modifier)
        VisualizerMode.SquircleBurst -> Unit
        VisualizerMode.EdgeBurst -> Unit
        VisualizerMode.EdgeBurstTaper -> Unit
        VisualizerMode.EdgeBurstButt -> Unit
        VisualizerMode.SpectrumRibbon -> SpectrumRibbonCanvas(frame, colors, coverFraction, modifier)
        VisualizerMode.LiquidBlob -> LiquidBlobCanvas(frame, colors, coverFraction, modifier)
        VisualizerMode.Particles -> ParticlesCanvas(frame, colors, coverFraction, modifier)
        VisualizerMode.CoverPulse -> Unit
        VisualizerMode.BeatFlash -> BeatFlashCanvas(frame, colors, modifier)
        VisualizerMode.WaveFloor -> WaveFloorCanvas(frame, colors, modifier)
    }
}

/** Full circular cover: faint rim + waves radiating outward (into the corners). */
@Composable
private fun AuraRingCircleCanvas(
    frame: SpectrumFrame,
    colors: VisualizerColors,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val half = min(size.width, size.height) / 2f
        val center = Offset(cx, cy)
        val energy = (frame.rms * 0.55f + frame.beatPulse * 0.45f).coerceIn(0f, 1f)
        // Source ring near cover edge — mostly transparent.
        val rimR = half * (0.97f - frame.rms * 0.04f - frame.beatPulse * 0.03f)
        val rimStroke = (half * 0.012f).coerceAtLeast(2f)
        drawCircle(
            color = colors.primary.copy(alpha = 0.12f + energy * 0.10f),
            radius = rimR,
            center = center,
            style = Stroke(width = rimStroke, cap = StrokeCap.Round),
        )
        // Ripples expand from the rim into the square corners.
        val maxR = half * 1.38f
        drawRadiatingWaves(
            center = center,
            originR = rimR,
            maxR = maxR,
            energy = energy,
            beat = frame.beatPulse,
            timestampMs = frame.timestampMs,
            color = colors.accent,
            secondary = colors.primary,
            waveCount = 4,
            strokeScale = half,
        )
    }
}

/** Square cover: faint core ring + waves expanding from the center. */
@Composable
private fun AuraRingCenterCanvas(
    frame: SpectrumFrame,
    colors: VisualizerColors,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val half = min(size.width, size.height) / 2f
        val center = Offset(cx, cy)
        val energy = (frame.rms * 0.55f + frame.beatPulse * 0.45f).coerceIn(0f, 1f)
        val coreR = half * (0.10f + energy * 0.06f)
        drawCircle(
            color = colors.primary.copy(alpha = 0.10f + energy * 0.12f),
            radius = coreR,
            center = center,
            style = Stroke(width = (half * 0.01f).coerceAtLeast(2f), cap = StrokeCap.Round),
        )
        drawRadiatingWaves(
            center = center,
            originR = coreR,
            maxR = half * 0.92f,
            energy = energy,
            beat = frame.beatPulse,
            timestampMs = frame.timestampMs,
            color = colors.accent,
            secondary = colors.primary,
            waveCount = 5,
            strokeScale = half,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadiatingWaves(
    center: Offset,
    originR: Float,
    maxR: Float,
    energy: Float,
    beat: Float,
    timestampMs: Long,
    color: Color,
    secondary: Color,
    waveCount: Int,
    strokeScale: Float,
) {
    if (energy < 0.02f && beat < 0.02f) return
    val span = (maxR - originR).coerceAtLeast(1f)
    val speed = 0.55f + beat * 0.35f
    val t = (timestampMs % 100_000L) / 1000f
    val baseAlpha = (0.25f + energy * 0.55f).coerceIn(0.15f, 0.85f)
    for (w in 0 until waveCount) {
        val phase = ((t * speed + w.toFloat() / waveCount) % 1f)
        val radius = originR + phase * span
        val fade = (1f - phase)
        val alpha = (baseAlpha * fade * fade).coerceIn(0f, 0.8f)
        if (alpha < 0.03f) continue
        val stroke = strokeScale * (0.014f + (1f - phase) * 0.022f + beat * 0.01f)
        val tint = if (w % 2 == 0) color else secondary
        drawCircle(
            color = tint.copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(width = stroke.coerceAtLeast(2f), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun RadialBarsCanvas(
    frame: SpectrumFrame,
    colors: VisualizerColors,
    coverFraction: Float,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val half = min(size.width, size.height) / 2f
        val inner = half * coverFraction + half * 0.03f
        val maxLen = half * (1f - coverFraction) * 1.15f
        val barCount = frame.bands.size
        val stroke = (half * 0.018f).coerceIn(3f, 8f)
        for (i in 0 until barCount) {
            val angle = (i / barCount.toFloat()) * PI.toFloat() * 2f - PI.toFloat() / 2f
            val len = (0.12f + frame.bands[i] * 0.88f) * maxLen
            val x0 = cx + cos(angle) * inner
            val y0 = cy + sin(angle) * inner
            val x1 = cx + cos(angle) * (inner + len)
            val y1 = cy + sin(angle) * (inner + len)
            drawLine(
                color = colors.primary.copy(alpha = 0.55f + frame.bands[i] * 0.45f),
                start = Offset(x0, y0),
                end = Offset(x1, y1),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Circular cover: bars from the rim inward.
 * Spectrum around the circle is mirrored 0→1→0 (top = bass, bottom = highs, top = bass).
 */
@Composable
private fun RadialBarsInwardCanvas(
    frame: SpectrumFrame,
    colors: VisualizerColors,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val half = min(size.width, size.height) / 2f
        val outer = half * 0.98f
        val maxLen = half * 0.62f
        // Even count keeps left/right halves symmetric for the 0-1-0 map.
        val barCount = (frame.bands.size * 2).coerceAtMost(96)
        val stroke = (2f * PI.toFloat() * outer / barCount * 0.55f).coerceIn(2.5f, 7f)
        val lastBand = (frame.bands.size - 1).coerceAtLeast(0)
        for (i in 0 until barCount) {
            val frac = i / barCount.toFloat()
            // 0 at top → 1 at bottom → 0 at top (closed circle).
            val spectrumT = if (frac <= 0.5f) frac * 2f else (1f - frac) * 2f
            val level = sampleBand(frame.bands, spectrumT * lastBand)
            val angle = frac * PI.toFloat() * 2f - PI.toFloat() / 2f
            val len = (0.06f + level * 0.94f) * maxLen
            val x0 = cx + cos(angle) * outer
            val y0 = cy + sin(angle) * outer
            val x1 = cx + cos(angle) * (outer - len)
            val y1 = cy + sin(angle) * (outer - len)
            val tint = lerpColor(colors.primary, colors.accent, level * 0.65f)
                .copy(alpha = 0.55f + level * 0.4f)
            drawLine(
                color = tint,
                start = Offset(x0, y0),
                end = Offset(x1, y1),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Shrunken cover: mirrored 0→1→0 bars grow outward.
 * Gradient stays on the cover tint; tip only boosts alpha (no white wash).
 */
@Composable
private fun RadialBarsOutwardCanvas(
    frame: SpectrumFrame,
    colors: VisualizerColors,
    coverFraction: Float,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val half = min(size.width, size.height) / 2f
        val inner = half * coverFraction + half * 0.02f
        val maxLen = half * (1f - coverFraction) * 1.45f
        val barCount = (frame.bands.size * 2).coerceAtMost(96)
        val stroke = (2f * PI.toFloat() * inner / barCount * 0.55f).coerceIn(2.5f, 8f)
        val lastBand = (frame.bands.size - 1).coerceAtLeast(0)
        for (i in 0 until barCount) {
            val frac = i / barCount.toFloat()
            val spectrumT = if (frac <= 0.5f) frac * 2f else (1f - frac) * 2f
            val level = sampleBand(frame.bands, spectrumT * lastBand)
            val angle = frac * PI.toFloat() * 2f - PI.toFloat() / 2f
            val len = (0.08f + level * 0.92f) * maxLen
            val start = Offset(cx + cos(angle) * inner, cy + sin(angle) * inner)
            val end = Offset(cx + cos(angle) * (inner + len), cy + sin(angle) * (inner + len))
            val base = colors.primary.copy(alpha = 0.55f + level * 0.25f)
            val tip = colors.accent.copy(alpha = 0.85f + level * 0.15f)
            val mid = lerpColor(colors.primary, colors.accent, 0.5f)
                .copy(alpha = 0.7f + level * 0.2f)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(base, mid, tip),
                    start = start,
                    end = end,
                ),
                start = start,
                end = end,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun sampleBand(bands: FloatArray, index: Float): Float {
    if (bands.isEmpty()) return 0f
    val i0 = index.toInt().coerceIn(0, bands.lastIndex)
    val i1 = (i0 + 1).coerceAtMost(bands.lastIndex)
    val t = (index - i0).coerceIn(0f, 1f)
    return bands[i0] * (1f - t) + bands[i1] * t
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val x = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * x,
        green = a.green + (b.green - a.green) * x,
        blue = a.blue + (b.blue - a.blue) * x,
        alpha = a.alpha + (b.alpha - a.alpha) * x,
    )
}

@Composable
private fun SpectrumRibbonCanvas(
    frame: SpectrumFrame,
    colors: VisualizerColors,
    coverFraction: Float,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val half = min(size.width, size.height) / 2f
        val baseR = half * coverFraction + half * 0.05f
        val path = Path()
        val n = frame.bands.size.coerceAtLeast(2)
        for (i in 0 until n) {
            val t = i / (n - 1).toFloat()
            val angle = (-PI / 2 + t * PI).toFloat()
            val amp = baseR + half * 0.18f * frame.bands[i]
            val x = cx + cos(angle) * amp
            val y = cy + sin(angle) * amp
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = colors.accent.copy(alpha = 0.9f),
            style = Stroke(width = 4f + frame.rms * 10f, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun LiquidBlobCanvas(
    frame: SpectrumFrame,
    colors: VisualizerColors,
    coverFraction: Float,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val half = min(size.width, size.height) / 2f
        // Blob sits behind cover and spills into the ring area.
        val base = half * (coverFraction + 0.15f)
        val path = Path()
        val steps = 48
        for (i in 0..steps) {
            val a = i / steps.toFloat() * PI.toFloat() * 2f
            val band = frame.bands[(i * frame.bands.size / steps).coerceIn(0, frame.bands.lastIndex)]
            val r = base * (0.9f + band * 0.35f + frame.beatPulse * 0.08f)
            val x = cx + cos(a) * r
            val y = cy + sin(a) * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(
                    colors.primary.copy(alpha = 0.65f),
                    colors.secondary.copy(alpha = 0.35f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = half,
            ),
        )
    }
}

@Composable
private fun ParticlesCanvas(
    frame: SpectrumFrame,
    colors: VisualizerColors,
    coverFraction: Float,
    modifier: Modifier,
) {
    val seeds = remember { FloatArray(40) { Random.nextFloat() } }
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val half = min(size.width, size.height) / 2f
        val inner = half * coverFraction
        val outer = half * 0.98f
        for (i in seeds.indices) {
            val angle = seeds[i] * PI.toFloat() * 2f
            val band = frame.bands[i % frame.bands.size]
            val dist = inner + (outer - inner) * (0.15f + band * 0.85f)
            val x = cx + cos(angle) * dist
            val y = cy + sin(angle) * dist
            val radius = 3f + band * 12f + frame.beatPulse * 5f
            drawCircle(
                color = colors.accent.copy(alpha = 0.35f + band * 0.55f),
                radius = radius,
                center = Offset(x, y),
            )
        }
    }
}

@Composable
private fun BeatFlashCanvas(frame: SpectrumFrame, colors: VisualizerColors, modifier: Modifier) {
    Canvas(modifier) {
        val alpha = frame.beatPulse * 0.55f
        if (alpha < 0.02f) return@Canvas
        drawRect(color = colors.accent.copy(alpha = alpha))
    }
}

@Composable
private fun WaveFloorCanvas(frame: SpectrumFrame, colors: VisualizerColors, modifier: Modifier) {
    Canvas(modifier) {
        val n = frame.bands.size
        if (n == 0) return@Canvas
        val barW = size.width / n
        val floor = size.height
        val maxH = size.height * 0.88f

        for (i in 0 until n) {
            val level = frame.bands[i]
            if (level < 0.02f) continue
            val h = level * maxH
            drawRoundRect(
                color = colors.primary.copy(alpha = 0.45f + level * 0.55f),
                topLeft = Offset(i * barW + barW * 0.15f, floor - h - size.height * 0.06f),
                size = Size(barW * 0.7f, h),
                cornerRadius = CornerRadius(4f, 4f),
            )
        }

        // High-contrast log scale (50 Hz … 14 kHz), drawn on top.
        val tickColor = Color.White.copy(alpha = 0.75f)
        val baseY = floor - 2f
        drawLine(
            color = tickColor.copy(alpha = 0.5f),
            start = Offset(0f, baseY),
            end = Offset(size.width, baseY),
            strokeWidth = 2f,
        )
        for (hz in SpectrumScale.MarkerHz) {
            val x = SpectrumScale.fractionForHz(hz) * size.width
            val major = hz == 100f || hz == 1_000f || hz == 8_000f
            val tickH = if (major) size.height * 0.22f else size.height * 0.12f
            drawLine(
                color = tickColor,
                start = Offset(x, baseY),
                end = Offset(x, baseY - tickH),
                strokeWidth = if (major) 2.5f else 1.5f,
            )
        }
    }
}
