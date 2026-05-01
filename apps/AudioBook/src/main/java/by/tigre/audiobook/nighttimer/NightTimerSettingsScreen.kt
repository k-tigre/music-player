package by.tigre.audiobook.nighttimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import by.tigre.audiobook.R

val NIGHT_TIMER_MINUTE_CHOICES: List<Int> = listOf(5, 10, 15, 20, 30)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NightTimerSettingsScreen(
    controller: NightTimerController,
    onBack: () -> Unit,
) {
    val selectedMinutes by controller.selectedMinutes.collectAsState()
    val fadeOut by controller.fadeOutAtEnd.collectAsState()
    val ui by controller.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.night_timer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.night_timer_back_cd),
                        )
                    }
                },
            )
        },
    ) { paddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.night_timer_minutes_label),
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NIGHT_TIMER_MINUTE_CHOICES.forEach { m ->
                    FilterChip(
                        selected = selectedMinutes == m,
                        onClick = { controller.setSelectedMinutes(m) },
                        label = { Text(stringResource(R.string.night_timer_minutes_option, m)) },
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.night_timer_fade_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.night_timer_fade_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(
                    checked = fadeOut,
                    onCheckedChange = controller::setFadeOutAtEnd,
                    modifier = Modifier.align(Alignment.End),
                )
            }

            if (ui.isRunning) {
                Button(
                    onClick = controller::cancelTimer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.night_timer_stop))
                }
            } else {
                Button(
                    onClick = controller::startTimer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.night_timer_start))
                }
            }
        }
    }
}
