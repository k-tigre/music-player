package by.tigre.media.platform.playback.impl

import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.impl.dsp.DesktopEqualizerPresets
import by.tigre.media.platform.playback.prefs.CustomEqPresetBank
import by.tigre.media.platform.playback.prefs.EqualizerPreferences
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
    private val firstCustom get() = builtInCount
    private val bandCount = DesktopEqualizerPresets.bandCentersHz.size
    private val customBank = CustomEqPresetBank(
        prefs = equalizerPrefs,
        bandCount = { bandCount },
    )

    private val _available = MutableStateFlow(true)
    override val isAvailable: StateFlow<Boolean> = _available.asStateFlow()

    private val _names = MutableStateFlow(builtInNamesPlusCustom())
    override val presetNames: StateFlow<List<String>> = _names.asStateFlow()

    private val _selected = MutableStateFlow(initialSelectedPresetFromPreferences())
    override val selectedPresetIndex: StateFlow<Int> = _selected.asStateFlow()

    private val _bandCenterHz = MutableStateFlow(DesktopEqualizerPresets.bandCentersHz.toList())
    override val bandCenterHz: StateFlow<List<Float>> = _bandCenterHz.asStateFlow()

    private val _bandGainDb = MutableStateFlow(initialBandGainsFromPreferences())
    override val bandGainDb: StateFlow<List<Float>> = _bandGainDb.asStateFlow()

    private val _builtInTable = MutableStateFlow(DesktopEqualizerPresets.allBuiltInBandGainsDb())
    override val builtInPresetBandGainsDb: StateFlow<List<List<Float>>> = _builtInTable.asStateFlow()

    private val _customPresetIndex = MutableStateFlow(firstCustom)
    override val customPresetIndex: StateFlow<Int> = _customPresetIndex.asStateFlow()

    private val _customPresetCount = MutableStateFlow(customBank.count)
    override val customPresetCount: StateFlow<Int> = _customPresetCount.asStateFlow()

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

    private fun builtInNamesPlusCustom(): List<String> =
        DesktopEqualizerPresets.names + customBank.titles

    private fun refreshCustomMeta() {
        _customPresetCount.value = customBank.count
        _names.value = builtInNamesPlusCustom()
    }

    private fun initialSelectedPresetFromPreferences(): Int {
        val saved = equalizerPrefs.loadSelectedPresetIndex(0)
        return saved.coerceIn(0, (firstCustom + customBank.count - 1).coerceAtLeast(0))
    }

    private fun initialBandGainsFromPreferences(): List<Float> {
        val idx = initialSelectedPresetFromPreferences()
        return if (idx < builtInCount) {
            DesktopEqualizerPresets.gainsForPreset(idx).toList()
        } else {
            customBank.gains(idx - firstCustom)
        }
    }

    override fun selectPreset(index: Int) {
        if (index !in _names.value.indices) return
        _selected.value = index
        equalizerPrefs.saveSelectedPresetIndex(index)
        when {
            index < builtInCount -> {
                _bandGainDb.value = DesktopEqualizerPresets.gainsForPreset(index).toList()
                scope.launch { player.applyEqualizerPreset(index) }
            }
            else -> {
                val gains = customBank.gains(index - firstCustom)
                _bandGainDb.value = gains
                scope.launch { player.applyEqualizerCustomGains(gains.toFloatArray()) }
            }
        }
    }

    override fun setBandGainDb(bandIndex: Int, gainDb: Float) {
        val bands = _bandGainDb.value
        if (bandIndex !in bands.indices) return
        val clamped = gainDb.coerceIn(DesktopEqualizerPresets.GAIN_DB_MIN, DesktopEqualizerPresets.GAIN_DB_MAX)
        val next = bands.toMutableList().also { it[bandIndex] = clamped }
        val selected = _selected.value
        val target = if (customBank.isCustomAbsolute(selected, firstCustom)) selected else firstCustom
        val slot = target - firstCustom
        _bandGainDb.value = next
        _selected.value = target
        equalizerPrefs.saveSelectedPresetIndex(target)
        customBank.updateGains(slot, next)
        refreshCustomMeta()
        scope.launch { player.applyEqualizerCustomGains(next.toFloatArray()) }
    }

    override fun addCustomPreset(): Boolean {
        val slot = customBank.addCopy(_bandGainDb.value)
        if (slot < 0) return false
        refreshCustomMeta()
        selectPreset(firstCustom + slot)
        return true
    }

    override fun renameCustomPreset(presetIndex: Int, title: String): Boolean {
        if (!customBank.isCustomAbsolute(presetIndex, firstCustom)) return false
        val ok = customBank.rename(presetIndex - firstCustom, title)
        if (ok) refreshCustomMeta()
        return ok
    }
}
