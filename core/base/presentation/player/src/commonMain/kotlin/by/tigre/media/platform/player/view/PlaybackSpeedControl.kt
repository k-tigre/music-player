package by.tigre.media.platform.player.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import by.tigre.media.platform.playback.PlaybackSpeed

enum class PlaybackSpeedControlLayout {
    Inline,
    Stacked,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedControl(
    speed: Float,
    label: String,
    resetLabel: String,
    onSpeedChange: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    layout: PlaybackSpeedControlLayout = PlaybackSpeedControlLayout.Inline,
) {
    when (layout) {
        PlaybackSpeedControlLayout.Inline -> InlinePlaybackSpeedControl(
            speed = speed,
            label = label,
            resetLabel = resetLabel,
            onSpeedChange = onSpeedChange,
            onReset = onReset,
            modifier = modifier,
        )

        PlaybackSpeedControlLayout.Stacked -> StackedPlaybackSpeedControl(
            speed = speed,
            label = label,
            resetLabel = resetLabel,
            onSpeedChange = onSpeedChange,
            onReset = onReset,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlinePlaybackSpeedControl(
    speed: Float,
    label: String,
    resetLabel: String,
    onSpeedChange: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = playbackSpeedSliderColors()
    val displaySpeed = PlaybackSpeed.format(speed)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 4.dp),
        )
        Slider(
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "$label $displaySpeed" },
            value = PlaybackSpeed.coerce(speed),
            onValueChange = { onSpeedChange(PlaybackSpeed.quantize(it)) },
            valueRange = PlaybackSpeed.MIN..PlaybackSpeed.MAX,
            steps = PlaybackSpeed.sliderSteps,
            colors = colors,
        )
        Text(
            text = displaySpeed,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        TextButton(onClick = onReset) {
            Text(text = resetLabel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StackedPlaybackSpeedControl(
    speed: Float,
    label: String,
    resetLabel: String,
    onSpeedChange: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = playbackSpeedSliderColors()
    val displaySpeed = PlaybackSpeed.format(speed)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = displaySpeed,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "$label $displaySpeed" },
            value = PlaybackSpeed.coerce(speed),
            onValueChange = { onSpeedChange(PlaybackSpeed.quantize(it)) },
            valueRange = PlaybackSpeed.MIN..PlaybackSpeed.MAX,
            steps = PlaybackSpeed.sliderSteps,
            colors = colors,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onReset) {
                Text(text = resetLabel)
            }
        }
    }
}

@Composable
private fun playbackSpeedSliderColors() = SliderDefaults.colors(
    activeTickColor = MaterialTheme.colorScheme.secondary,
    thumbColor = MaterialTheme.colorScheme.secondary,
    activeTrackColor = MaterialTheme.colorScheme.secondary,
    inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
)
