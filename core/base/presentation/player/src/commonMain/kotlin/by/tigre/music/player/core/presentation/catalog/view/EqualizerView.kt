package by.tigre.music.player.core.presentation.catalog.view

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.presentation.catalog.component.EqualizerComponent
import by.tigre.music.player.tools.platform.compose.ComposableView
import by.tigre.music.player.tools.platform.compose.resources.Res
import by.tigre.music.player.tools.platform.compose.resources.equalizer_bands
import by.tigre.music.player.tools.platform.compose.resources.equalizer_custom
import by.tigre.music.player.tools.platform.compose.resources.equalizer_factory_presets_table
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
        val factoryPresetsTableTitle = stringResource(Res.string.equalizer_factory_presets_table)
        val presetPickerTitle = stringResource(Res.string.equalizer_preset_picker)
        val bandsSectionTitle = stringResource(Res.string.equalizer_bands)
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
            val table by component.playbackEqualizer.builtInPresetBandGainsDb.collectAsState()
            val customIdx by component.playbackEqualizer.customPresetIndex.collectAsState()
            val gainRange by component.playbackEqualizer.bandGainRangeDb.collectAsState()

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (table.isNotEmpty() && centers.isNotEmpty()) {
                    Text(
                        text = factoryPresetsTableTitle,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    PresetGainsTable(
                        presetNames = presetNames,
                        bandLabels = centers.map(::formatBandHz),
                        rows = table,
                    )
                }

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

                Text(
                    text = bandsSectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                )

                centers.forEachIndexed { index, hz ->
                    val g = gains.getOrNull(index) ?: 0f
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatBandHz(hz) + " Hz",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = formatDb(g),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Slider(
                            value = g.coerceIn(gainRange.first, gainRange.second),
                            onValueChange = { component.playbackEqualizer.setBandGainDb(index, it) },
                            valueRange = gainRange.first..gainRange.second,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PresetGainsTable(
        presetNames: List<String>,
        bandLabels: List<String>,
        rows: List<List<Float>>,
    ) {
        val scroll = rememberScrollState()
        val colWidth = 44.dp
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .horizontalScroll(scroll),
            ) {
                Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(96.dp))
                    bandLabels.forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.width(colWidth),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                }
                rows.forEachIndexed { rowIndex, gains ->
                    if (rowIndex >= presetNames.lastIndex) return@forEachIndexed
                    Row(
                        modifier = Modifier.height(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = presetNames[rowIndex],
                            modifier = Modifier.width(96.dp),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        gains.forEach { db ->
                            Text(
                                text = formatDbCompact(db),
                                modifier = Modifier.width(colWidth),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
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
        if (db >= 0f) "+%.1f dB".format(db).replace(",", ".")
        else "%.1f dB".format(db).replace(",", ".")

    private fun formatDbCompact(db: Float): String =
        if (db >= 0f) "+%.0f".format(db).replace(",", ".")
        else "%.0f".format(db).replace(",", ".")
}
