package by.tigre.audiobook.playback

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import by.tigre.audiobook.R
import by.tigre.media.platform.player.component.BasePlayerComponent
import by.tigre.media.platform.player.view.PlaybackSpeedControl
import by.tigre.media.platform.player.view.PlaybackSpeedControlLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedSettingsScreen(
    playerComponent: BasePlayerComponent,
    onBack: () -> Unit,
) {
    val playbackSpeedFlow = requireNotNull(playerComponent.playbackSpeed) {
        "Playback speed settings require playbackSpeedSource"
    }
    val speed by playbackSpeedFlow.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.playback_speed_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.playback_speed_back_cd),
                        )
                    }
                },
            )
        },
    ) { paddings ->
        PlaybackSpeedControl(
            speed = speed,
            label = stringResource(R.string.player_playback_speed_label),
            resetLabel = stringResource(R.string.player_playback_speed_reset),
            onSpeedChange = playerComponent::setPlaybackSpeed,
            onReset = playerComponent::resetPlaybackSpeed,
            layout = PlaybackSpeedControlLayout.Stacked,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
