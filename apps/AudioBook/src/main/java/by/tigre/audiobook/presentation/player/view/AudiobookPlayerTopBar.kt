package by.tigre.audiobook.presentation.player.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.tigre.audiobook.R
import by.tigre.audiobook.nighttimer.NightTimerController
import by.tigre.media.platform.playback.PlaybackSpeed
import by.tigre.media.platform.player.component.PlayerComponent

@Composable
fun AudiobookPlayerTopBar(
    playerComponent: PlayerComponent,
    nightTimerController: NightTimerController,
    onShowCatalog: () -> Unit,
    onOpenFolderSettings: () -> Unit,
    onOpenNightTimerSettings: () -> Unit,
    onOpenPlaybackSpeedSettings: () -> Unit,
    onShowEqualizer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentItem by playerComponent.currentItem.collectAsState()
    val eqAvailable by playerComponent.playbackEqualizer.isAvailable.collectAsState()
    val nightUi by nightTimerController.uiState.collectAsState()
    val speed by requireNotNull(playerComponent.playbackSpeed).collectAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onShowCatalog) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.player_back_to_library_cd),
            )
        }

        Text(
            text = currentItem?.subtitle.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onOpenPlaybackSpeedSettings) {
                Text(
                    text = PlaybackSpeed.format(speed),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            if (nightUi.isRunning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            R.string.night_timer_countdown_short,
                            nightUi.remainingSeconds / 60,
                            nightUi.remainingSeconds % 60,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.widthIn(max = 72.dp),
                        maxLines = 1,
                    )
                    IconButton(
                        onClick = nightTimerController::cancelTimer,
                        modifier = Modifier.padding(start = (-8).dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.night_timer_cancel_cd),
                        )
                    }
                }
            } else {
                IconButton(onClick = onOpenNightTimerSettings) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = stringResource(R.string.player_open_night_timer_cd),
                    )
                }
            }

            IconButton(onClick = onOpenFolderSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.player_open_folder_settings_cd),
                )
            }

            if (eqAvailable) {
                IconButton(onClick = onShowEqualizer) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = stringResource(R.string.player_open_equalizer_cd),
                    )
                }
            }
        }
    }
}
