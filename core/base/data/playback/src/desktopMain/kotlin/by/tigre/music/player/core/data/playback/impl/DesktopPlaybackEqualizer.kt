package by.tigre.music.player.core.data.playback.impl

import by.tigre.music.player.core.data.playback.PlaybackEqualizer
import by.tigre.music.player.core.data.playback.impl.dsp.DesktopEqualizerPresets
import by.tigre.music.player.core.data.playback.prefs.EqualizerPreferences
import by.tigre.music.player.core.data.playback.prefs.alignGainsToBandCount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class DesktopPlaybackEqualizer(
    private val player: JdkClipDesktopPlaybackPlayer,
    private val equalizerPrefs: EqualizerPreferences,
) : PlaybackEqualizer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val builtInCount = DesktopEqualizerPresets.names.size
    private val customIdx get() = builtInCount

    private val bandCount = DesktopEqualizerPresets.bandCentersHz.size

    private val _available = MutableStateFlow(true)
    override val isAvailable: StateFlow<Boolean> = _available.asStateFlow()

    private val _names = MutableStateFlow(DesktopEqualizerPresets.names + "Custom")
    override val presetNames: StateFlow<List<String>> = _names.asStateFlow()

    private val _selected = MutableStateFlow(initialSelectedPresetFromPreferences())
    override val selectedPresetIndex: StateFlow<Int> = _selected.asStateFlow()

    private val _bandCenterHz = MutableStateFlow(DesktopEqualizerPresets.bandCentersHz.toList())
    override val bandCenterHz: StateFlow<List<Float>> = _bandCenterHz.asStateFlow()

    private val _bandGainDb = MutableStateFlow(initialBandGainsFromPreferences())
    override val bandGainDb: StateFlow<List<Float>> = _bandGainDb.asStateFlow()

    private val _builtInTable = MutableStateFlow(DesktopEqualizerPresets.allBuiltInBandGainsDb())
    override val builtInPresetBandGainsDb: StateFlow<List<List<Float>>> = _builtInTable.asStateFlow()

    private val _customPresetIndex = MutableStateFlow(customIdx)
    override val customPresetIndex: StateFlow<Int> = _customPresetIndex.asStateFlow()

    private val _bandGainRangeDb =
        MutableStateFlow(DesktopEqualizerPresets.GAIN_DB_MIN to DesktopEqualizerPresets.GAIN_DB_MAX)
    override val bandGainRangeDb: StateFlow<Pair<Float, Float>> = _bandGainRangeDb.asStateFlow()

    init {
        val idx = _selected.value
        scope.launch {
            if (idx < builtInCount) {
                player.applyEqualizerPreset(idx)
            } else {
                player.applyEqualizerCustomGains(_bandGainDb.value.toFloatArray())
            }
        }
    }

    private fun initialSelectedPresetFromPreferences(): Int {
        val saved = equalizerPrefs.loadSelectedPresetIndex(0)
        return saved.coerceIn(0, customIdx)
    }

    private fun initialBandGainsFromPreferences(): List<Float> {
        val idx = initialSelectedPresetFromPreferences()
        return if (idx < builtInCount) {
            DesktopEqualizerPresets.gainsForPreset(idx).toList()
        } else {
            alignGainsToBandCount(equalizerPrefs.loadCustomBandGainsDb(), bandCount)
        }
    }

    override fun selectPreset(index: Int) {
        if (index !in _names.value.indices) return
        _selected.value = index
        equalizerPrefs.saveSelectedPresetIndex(index)
        when {
            index < builtInCount -> {
                _bandGainDb.value = DesktopEqualizerPresets.gainsForPreset(index).toList()
                scope.launch {
                    player.applyEqualizerPreset(index)
                }
            }

            else -> {
                val aligned = alignGainsToBandCount(equalizerPrefs.loadCustomBandGainsDb(), bandCount)
                _bandGainDb.value = aligned
                equalizerPrefs.saveCustomBandGainsDb(aligned)
                scope.launch {
                    player.applyEqualizerCustomGains(aligned.toFloatArray())
                }
            }
        }
    }

    override fun setBandGainDb(bandIndex: Int, gainDb: Float) {
        val bands = _bandGainDb.value
        if (bandIndex !in bands.indices) return
        val clamped = gainDb.coerceIn(DesktopEqualizerPresets.GAIN_DB_MIN, DesktopEqualizerPresets.GAIN_DB_MAX)
        val next = bands.toMutableList().also { it[bandIndex] = clamped }
        _bandGainDb.value = next
        _selected.value = customIdx
        equalizerPrefs.saveSelectedPresetIndex(customIdx)
        equalizerPrefs.saveCustomBandGainsDb(next)
        scope.launch {
            player.applyEqualizerCustomGains(next.toFloatArray())
        }
    }
}
