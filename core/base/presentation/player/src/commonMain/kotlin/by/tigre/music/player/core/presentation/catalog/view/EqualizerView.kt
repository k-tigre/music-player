package by.tigre.music.player.core.presentation.catalog.view

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.presentation.catalog.component.EqualizerComponent
import by.tigre.music.player.tools.platform.compose.ComposableView
import by.tigre.music.player.tools.platform.compose.resources.Res
import by.tigre.music.player.tools.platform.compose.resources.equalizer_custom
import by.tigre.music.player.tools.platform.compose.resources.equalizer_preset_picker
import by.tigre.music.player.tools.platform.compose.resources.equalizer_title
import by.tigre.music.player.tools.platform.compose.resources.equalizer_unavailable
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

class EqualizerView(
    private val component: EqualizerComponent,
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        val available by component.playbackEqualizer.isAvailable.collectAsState()
        val title = stringResource(Res.string.equalizer_title)
        val presetPickerTitle = stringResource(Res.string.equalizer_preset_picker)
        val customPresetLabel = stringResource(Res.string.equalizer_custom)
        val unavailableMessage = stringResource(Res.string.equalizer_unavailable)

        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = component::close) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                )
            },
        ) { padding ->
            if (!available) {
                Text(
                    text = unavailableMessage,
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
                return@Scaffold
            }

            val presetNames by component.playbackEqualizer.presetNames.collectAsState()
            val selected by component.playbackEqualizer.selectedPresetIndex.collectAsState()
            val centers by component.playbackEqualizer.bandCenterHz.collectAsState()
            val gains by component.playbackEqualizer.bandGainDb.collectAsState()
            val customIdx by component.playbackEqualizer.customPresetIndex.collectAsState()
            val gainRange by component.playbackEqualizer.bandGainRangeDb.collectAsState()

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = presetPickerTitle,
                    style = MaterialTheme.typography.titleMedium,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presetNames.forEachIndexed { index, name ->
                        val label =
                            if (index == customIdx && customPresetLabel.isNotEmpty()) {
                                customPresetLabel
                            } else {
                                name
                            }
                        FilterChip(
                            selected = selected == index,
                            onClick = { component.playbackEqualizer.selectPreset(index) },
                            label = {
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    centers.forEachIndexed { index, hz ->
                        val g = gains.getOrNull(index) ?: 0f
                        EqBandFaderColumn(
                            hzLabel = formatBandHz(hz),
                            gainDb = g.coerceIn(gainRange.first, gainRange.second),
                            gainRange = gainRange.first..gainRange.second,
                            onGainChange = { component.playbackEqualizer.setBandGainDb(index, it) },
                            modifier = Modifier
                                .width(48.dp)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun EqBandFaderColumn(
        hzLabel: String,
        gainDb: Float,
        gainRange: ClosedFloatingPointRange<Float>,
        onGainChange: (Float) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = formatDb(gainDb),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            VerticalGainFader(
                value = gainDb,
                onValueChange = onGainChange,
                valueRange = gainRange,
                modifier = Modifier
                    .weight(1f)
                    .width(40.dp),
            )
            Text(
                text = hzLabel,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }

    @Composable
    private fun VerticalGainFader(
        value: Float,
        onValueChange: (Float) -> Unit,
        valueRange: ClosedFloatingPointRange<Float>,
        modifier: Modifier = Modifier,
    ) {
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        val activeFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        val thumbColor = MaterialTheme.colorScheme.primary
        val zeroLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        val density = LocalDensity.current
        val minV = valueRange.start
        val maxV = valueRange.endInclusive

        BoxWithConstraints(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(trackColor),
        ) {
            val hPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

            fun valueFromY(y: Float, heightPx: Float): Float {
                val h = heightPx.coerceAtLeast(1f)
                val t = (y / h).coerceIn(0f, 1f)
                return maxV - t * (maxV - minV)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(minV, maxV) {
                        detectTapGestures { offset ->
                            onValueChange(valueFromY(offset.y, size.height.toFloat()))
                        }
                    }
                    .pointerInput(minV, maxV) {
                        detectVerticalDragGestures { change, _ ->
                            onValueChange(valueFromY(change.position.y, size.height.toFloat()))
                        }
                    },
            ) {
                val clamped = value.coerceIn(minV, maxV)
                val thumbFraction = (maxV - clamped) / (maxV - minV)
                val thumbCenterY = thumbFraction * hPx
                val zeroInRange = 0f in minV..maxV

                if (zeroInRange) {
                    val y0 = (maxV - 0f) / (maxV - minV) * hPx
                    val barTopPx = minOf(y0, thumbCenterY)
                    val barHeightPx = maxOf(abs(thumbCenterY - y0), with(density) { 3.dp.toPx() })
                    Box(
                        Modifier
                            .fillMaxWidth(0.42f)
                            .align(Alignment.TopCenter)
                            .offset(y = with(density) { barTopPx.toDp() })
                            .height(with(density) { barHeightPx.toDp() })
                            .clip(RoundedCornerShape(2.dp))
                            .background(activeFillColor),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = with(density) { (y0 - 1.dp.toPx()).toDp() })
                            .background(zeroLineColor),
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth(0.42f)
                            .align(Alignment.TopCenter)
                            .fillMaxHeight(thumbFraction.coerceIn(0.03f, 1f))
                            .background(activeFillColor),
                    )
                }

                val thumbDp = 20.dp
                val thumbRadiusPx = with(density) { (thumbDp / 2).toPx() }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(
                            y = with(density) {
                                (thumbCenterY - thumbRadiusPx).toDp().coerceAtLeast(0.dp)
                            },
                        )
                        .size(thumbDp)
                        .clip(CircleShape)
                        .background(thumbColor),
                )
            }
        }
    }

    private fun formatBandHz(hz: Float): String =
        when {
            hz >= 1000f && abs(hz - hz.roundToInt().toFloat()) < 0.5f ->
                (hz / 1000f).roundToInt().toString() + "k"

            hz >= 1000f -> "%.1fk".format(hz / 1000f).replace(",", ".")
            else -> "%.0f".format(hz)
        }

    private fun formatDb(db: Float): String =
        if (db >= 0f) "+%.1f".format(db).replace(",", ".")
        else "%.1f".format(db).replace(",", ".")
}
