package by.tigre.media.platform.player.view.visualizer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import by.tigre.media.platform.playback.AudioSpectrumSource
import by.tigre.media.platform.playback.VisualizerMode
import by.tigre.media.platform.playback.VisualizerSlot
import by.tigre.media.platform.playback.prefs.VisualizerPreferences
import by.tigre.media.platform.player.view.rememberCoverImageBitmap
import by.tigre.media.platform.player.view.rememberDominantCoverColor
import kotlin.math.min

/**
 * Soft circular edge: opaque in the center, fades to transparent near the rim.
 * Full-rect [DstIn] mask so square corners become transparent too.
 *
 * @param opaqueUntil fraction of radius that stays fully opaque (rest feathers out).
 * @param radiusScale >1 expands the mask so less of the cover is cropped at the rim.
 */
fun Modifier.softCircleEdge(
    opaqueUntil: Float = 0.78f,
    radiusScale: Float = 1f,
): Modifier =
    this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val r = min(size.width, size.height) / 2f * radiusScale.coerceAtLeast(1f)
            val center = Offset(size.width / 2f, size.height / 2f)
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        opaqueUntil.coerceIn(0.5f, 0.98f) to Color.White,
                        1f to Color.Transparent,
                    ),
                    center = center,
                    radius = r,
                ),
                blendMode = BlendMode.DstIn,
            )
        }

/** Soft fade on all four sides of a square cover (EdgeBurst / ambilight). */
fun Modifier.softSquareEdge(opaqueUntil: Float = 0.88f): Modifier =
    this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val fade = (1f - opaqueUntil.coerceIn(0.5f, 0.96f)).coerceIn(0.04f, 0.45f)
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        fade to Color.White,
                        1f - fade to Color.White,
                        1f to Color.Transparent,
                    ),
                ),
                blendMode = BlendMode.DstIn,
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        fade to Color.White,
                        1f - fade to Color.White,
                        1f to Color.Transparent,
                    ),
                ),
                blendMode = BlendMode.DstIn,
            )
        }

/**
 * Full-player visualizer host. [VisualizerSlot.Mini] is reserved and unused in v1.
 */
@Composable
fun PlayerVisualizerHost(
    coverModel: Any?,
    spectrumSource: AudioSpectrumSource,
    visualizerPreferences: VisualizerPreferences,
    slot: VisualizerSlot = VisualizerSlot.Full,
    coverContent: @Composable (coverModifier: Modifier) -> Unit,
) {
    if (slot != VisualizerSlot.Full) {
        coverContent(Modifier.fillMaxWidth().aspectRatio(1f))
        return
    }

    val sandbox = rememberVisualizerSandboxEnabled()
    val storedMode by visualizerPreferences.mode.collectAsState()
    val mode = if (sandbox) storedMode else VisualizerMode.Off
    val frame by spectrumSource.frames.collectAsState()
    val dominant = rememberDominantCoverColor(coverModel)
    val artwork = rememberCoverImageBitmap(coverModel)
    val scheme = MaterialTheme.colorScheme
    val activeFrame = frame ?: emptySpectrumFrame()
    val base = dominant ?: scheme.primary
    val (barBase, barTip) = base.boostForDarkBackdrop()
    val colors = VisualizerColors(
        primary = barBase,
        secondary = lerp(barBase, scheme.surface, 0.12f),
        accent = barTip,
    )

    DisposableEffect(mode, sandbox) {
        val enable = sandbox && mode != VisualizerMode.Off
        spectrumSource.setEnabled(enable)
        onDispose {
            spectrumSource.setEnabled(false)
        }
    }

    val pulseScale =
        if (mode == VisualizerMode.CoverPulse) {
            1f + activeFrame.rms * 0.06f + activeFrame.beatPulse * 0.04f
        } else {
            1f
        }
    val layoutScale = mode.coverScale()
    val circleCover = mode.clipsCoverToCircle()

    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        if (mode == VisualizerMode.SquircleBurst) {
            val coverMod = Modifier
                .fillMaxSize(fraction = 0.62f)
                .clip(RoundedCornerShape(6.dp))
            if (artwork != null) {
                SquircleBurstVisualizer(
                    amplitudes = activeFrame.bands,
                    artwork = artwork,
                    modifier = Modifier.fillMaxSize(),
                    baseInset = 0.62f,
                ) {
                    coverContent(coverMod)
                }
            } else {
                coverContent(coverMod)
            }
        } else if (
            mode == VisualizerMode.EdgeBurst ||
            mode == VisualizerMode.EdgeBurstTaper ||
            mode == VisualizerMode.EdgeBurstButt
        ) {
            val artFraction = when (mode) {
                VisualizerMode.EdgeBurst -> 0.74f
                else -> 0.55f
            }
            val softCover: @Composable () -> Unit = {
                Box(
                    modifier = Modifier.fillMaxSize(fraction = artFraction),
                    contentAlignment = Alignment.Center,
                ) {
                    coverContent(
                        Modifier
                            .fillMaxSize()
                            .scale(1.1f)
                            .blur(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .softSquareEdge(opaqueUntil = 0.52f),
                    )
                    coverContent(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .softSquareEdge(opaqueUntil = 0.90f),
                    )
                }
            }
            if (artwork != null) {
                EdgeBurstVisualizer(
                    amplitudes = activeFrame.bands,
                    artwork = artwork,
                    modifier = Modifier.fillMaxSize(),
                    artInset = artFraction,
                    barThicknessFraction = when (mode) {
                        VisualizerMode.EdgeBurstButt, VisualizerMode.EdgeBurstTaper -> 0.0045f
                        else -> 0.005f
                    },
                    barSpacingFraction = when (mode) {
                        VisualizerMode.EdgeBurstButt -> 0.0011f
                        VisualizerMode.EdgeBurstTaper -> 0.0012f
                        else -> 0.0025f
                    },
                    minBarLengthFraction = when (mode) {
                        VisualizerMode.EdgeBurstButt, VisualizerMode.EdgeBurstTaper -> 0.012f
                        else -> 0.015f
                    },
                    sampleRadius = when (mode) {
                        VisualizerMode.EdgeBurstButt, VisualizerMode.EdgeBurstTaper -> 3
                        else -> 2
                    },
                    edgeTaperPower = when (mode) {
                        VisualizerMode.EdgeBurstTaper -> 3f
                        VisualizerMode.EdgeBurstButt -> 2.5f
                        else -> null
                    },
                    cornerFloor = 0f,
                    barCap = when (mode) {
                        VisualizerMode.EdgeBurstButt -> StrokeCap.Butt
                        else -> StrokeCap.Round
                    },
                ) {
                    softCover()
                }
            } else {
                softCover()
            }
        } else {
            if (mode.isSurroundedUnderlay()) {
                VisualizerCanvas(
                    mode = mode,
                    frame = activeFrame,
                    colors = colors,
                    coverFraction = layoutScale,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (circleCover) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(layoutScale * pulseScale),
                    contentAlignment = Alignment.Center,
                ) {
                    coverContent(
                        Modifier
                            .fillMaxSize()
                            .scale(1.16f)
                            .blur(32.dp)
                            .softCircleEdge(opaqueUntil = 0.42f, radiusScale = 1f),
                    )
                    coverContent(
                        Modifier
                            .fillMaxSize()
                            .scale(1.06f)
                            .blur(14.dp)
                            .softCircleEdge(opaqueUntil = 0.58f, radiusScale = 1f),
                    )
                    coverContent(
                        Modifier
                            .fillMaxSize()
                            .softCircleEdge(opaqueUntil = 0.78f, radiusScale = 1f),
                    )
                }
            } else {
                coverContent(
                    Modifier
                        .fillMaxSize()
                        .scale(layoutScale * pulseScale),
                )
            }

            if (mode.isCoverOverlay()) {
                VisualizerCanvas(
                    mode = mode,
                    frame = activeFrame,
                    colors = colors,
                    coverFraction = 1f,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (mode == VisualizerMode.WaveFloor) {
                VisualizerCanvas(
                    mode = mode,
                    frame = activeFrame,
                    colors = colors,
                    coverFraction = 1f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.4f)
                        .align(Alignment.BottomCenter),
                )
            }
        }
    }
}
