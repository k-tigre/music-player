package by.tigre.music.player.core.presentation.catalog.view

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProgressSlider(
    fraction: Float,
    onSeekTo: (Float) -> Unit,
    onSeekCommitted: (Float) -> Unit = { },
    modifier: Modifier = Modifier,
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var sliderEnabled by remember { mutableStateOf(false) }
    var gestureChangedValue by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(
        activeTickColor = MaterialTheme.colorScheme.secondary,
        thumbColor = MaterialTheme.colorScheme.secondary,
        activeTrackColor = MaterialTheme.colorScheme.secondary,
        inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
    )

    Slider(
        modifier = modifier,
        value = if (sliderEnabled) sliderPosition else fraction,
        onValueChange = {
            sliderPosition = it
            sliderEnabled = true
            gestureChangedValue = true
            onSeekTo(it)
        },
        onValueChangeFinished = {
            if (gestureChangedValue) {
                onSeekCommitted(sliderPosition)
                gestureChangedValue = false
            }
            sliderEnabled = false
        },
        colors = colors,
        interactionSource = interactionSource,
        thumb = {
            Spacer(
                Modifier
                    .size(24.dp)
                    .hoverable(interactionSource = interactionSource)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
            )
        },
        track = { state ->
            SliderDefaults.Track(
                colors = colors,
                enabled = true,
                sliderState = state,
                thumbTrackGapSize = 0.dp,
                modifier = Modifier.height(8.dp),
                drawStopIndicator = null
            )
        }
    )
}
