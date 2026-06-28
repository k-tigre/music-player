package by.tigre.audiobook.debug.nighttimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import by.tigre.audiobook.nighttimer.NightTimerShakeConfig
import by.tigre.audiobook.nighttimer.NightTimerShakeConfigSource
import by.tigre.media.platform.tools.platform.compose.ComposableView
import kotlin.math.roundToInt

internal class NightTimerShakeDebugView(
    private val component: NightTimerShakeDebugComponent,
) : ComposableView {

    @Composable
    override fun Draw(modifier: Modifier) {
        val config by component.shakeConfig.collectAsState()
        val configSource by component.shakeConfigSource.collectAsState()
        val configFetching by component.shakeConfigFetching.collectAsState()
        val state by component.debugState.collectAsState()

        DisposableEffect(Unit) {
            onDispose { component.setTestMode(false) }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TestModeRow(
                testMode = state.testMode,
                onTestModeChange = component::setTestMode,
                onResetDetection = component::resetDetection,
            )

            StatusCard(config = config, state = state)

            HorizontalDivider()

            ConfigSourceRow(
                source = configSource,
                fetching = configFetching,
                onRefresh = component::refreshConfig,
            )

            Text(
                text = "Debug override",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Sliders apply a local debug override. Production uses Firebase Remote Config.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ConfigSlider(
                label = "G-force threshold",
                value = config.gForceThreshold,
                valueRange = NightTimerShakeConfig.G_FORCE_MIN..NightTimerShakeConfig.G_FORCE_MAX,
                format = { "%.2f g".format(it) },
                onValueChange = component::updateGForceThreshold,
            )

            ConfigSlider(
                label = "Debounce",
                value = config.debounceMs.toFloat(),
                valueRange = NightTimerShakeConfig.DEBOUNCE_MIN_MS.toFloat()..NightTimerShakeConfig.DEBOUNCE_MAX_MS.toFloat(),
                format = { "${it.roundToInt()} ms" },
                onValueChange = { component.updateDebounceMs(it.roundToInt().toLong()) },
            )

            ConfigSlider(
                label = "Pair max gap",
                value = config.pairMaxGapMs.toFloat(),
                valueRange = NightTimerShakeConfig.PAIR_GAP_MIN_MS.toFloat()..NightTimerShakeConfig.PAIR_GAP_MAX_MS.toFloat(),
                format = { "${(it / 1000f).roundToInt()} s" },
                onValueChange = { component.updatePairMaxGapMs(it.roundToInt().toLong()) },
            )

            OutlinedButton(
                onClick = component::resetConfigToDefaults,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (configSource == NightTimerShakeConfigSource.DebugOverride) {
                        "Clear debug override"
                    } else {
                        "Use remote config"
                    },
                )
            }
        }
    }
}

@Composable
private fun ConfigSourceRow(
    source: NightTimerShakeConfigSource,
    fetching: Boolean,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Config source", style = MaterialTheme.typography.titleMedium)
            Text(
                text = when (source) {
                    NightTimerShakeConfigSource.Remote -> "Firebase Remote Config"
                    NightTimerShakeConfigSource.DebugOverride -> "Local debug override"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = onRefresh,
            enabled = !fetching,
        ) {
            Text(if (fetching) "Fetching…" else "Fetch remote")
        }
    }
}

@Composable
private fun TestModeRow(
    testMode: Boolean,
    onTestModeChange: (Boolean) -> Unit,
    onResetDetection: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Test mode", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Listen for shakes without a running timer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = testMode, onCheckedChange = onTestModeChange)
    }
    Button(onClick = onResetDetection, modifier = Modifier.fillMaxWidth()) {
        Text("Reset shake progress")
    }
}

@Composable
private fun StatusCard(
    config: NightTimerShakeConfig,
    state: by.tigre.audiobook.nighttimer.NightTimerShakeDebugState,
) {
    val aboveThreshold = state.currentGForce >= config.gForceThreshold

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Live status", style = MaterialTheme.typography.titleMedium)

            Text(
                text = "Sensor: ${if (state.sensorActive) "on" else "off"} · " +
                    "Timer gate: ${if (state.detectionEnabled) "open" else "closed"}",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = "G-force: %.2f g (threshold %.2f g)".format(state.currentGForce, config.gForceThreshold),
                style = MaterialTheme.typography.bodyLarge,
                color = if (aboveThreshold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )

            ShakeStepRow(
                label = "Shake 1",
                passed = state.firstShakePassed,
            )
            ShakeStepRow(
                label = "Shake 2",
                passed = state.completedPairs > 0 || state.pairPassed,
                pending = state.shakeCount == 1,
            )

            if (state.completedPairs > 0) {
                Text(
                    text = "Pairs completed: ${state.completedPairs}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2E7D32),
                )
            }

            if (state.pairPassed) {
                Text(
                    text = "Pair passed just now",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2E7D32),
                )
            }
        }
    }
}

@Composable
private fun ShakeStepRow(
    label: String,
    passed: Boolean,
    pending: Boolean = false,
) {
    val status = when {
        passed -> "✓"
        pending -> "…"
        else -> "○"
    }
    val color = when {
        passed -> Color(0xFF2E7D32)
        pending -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = status,
            color = color,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (passed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ConfigSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = format(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}
