package by.tigre.music.player.debug.visualizer

import android.Manifest
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import by.tigre.media.platform.playback.VisualizerMode
import by.tigre.media.platform.playback.VisualizerProcessing
import by.tigre.media.platform.tools.platform.compose.ComposableView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
internal class VisualizerDebugView(
    private val component: VisualizerDebugComponent,
) : ComposableView {

    @Composable
    override fun Draw(modifier: Modifier) {
        val selected by component.mode.collectAsState()
        val processing by component.processing.collectAsState()
        val recordPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
        val needsMic = selected != VisualizerMode.Off

        LaunchedEffect(needsMic, recordPermission.status.isGranted) {
            if (needsMic && !recordPermission.status.isGranted) {
                recordPermission.launchPermissionRequest()
            }
        }

        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = "Full player visualizer (debug). Needs RECORD_AUDIO for session capture.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            if (needsMic && !recordPermission.status.isGranted) {
                Text(
                    text = if (recordPermission.status.shouldShowRationale || Build.VERSION.SDK_INT >= 23) {
                        "RECORD_AUDIO not granted — Visualizer fails with error -3."
                    } else {
                        "RECORD_AUDIO required."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { recordPermission.launchPermissionRequest() }) {
                    Text("Grant RECORD_AUDIO")
                }
                Spacer(Modifier.height(16.dp))
            }

            Text("Processing", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Pretty = ComposeCircle-style (log + min-max). " +
                    "Realistic = Tee track loudness (dynamics, works with device mute).",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            VisualizerProcessing.entries.forEach { style ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = processing == style,
                            onClick = { component.setProcessing(style) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = processing == style,
                        onClick = { component.setProcessing(style) },
                    )
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(style.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = when (style) {
                                VisualizerProcessing.Pretty -> "Lively look; per-frame normalize"
                                VisualizerProcessing.Realistic -> "Follows song level via PCM Tee"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text("Effect", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            VisualizerMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected == mode,
                            onClick = { component.setMode(mode) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selected == mode,
                        onClick = { component.setMode(mode) },
                    )
                    Text(
                        text = when (mode) {
                            VisualizerMode.AuraRingCircle -> "AuraRing · circle (waves)"
                            VisualizerMode.AuraRingCenter -> "AuraRing · from center"
                            VisualizerMode.RadialBarsInward -> "RadialBars · inward (0-1-0)"
                            VisualizerMode.RadialBarsOutward -> "RadialBars · outward (0-1-0)"
                            VisualizerMode.SquircleBurst -> "SquircleBurst · artwork colors"
                            VisualizerMode.EdgeBurst -> "EdgeBurst · ambilight"
                            VisualizerMode.EdgeBurstTaper -> "EdgeBurst · tapered sunburst"
                            VisualizerMode.EdgeBurstButt -> "EdgeBurst · tapered butt"
                            else -> mode.name
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { component.setMode(mode) },
                    )
                }
            }
        }
    }
}
