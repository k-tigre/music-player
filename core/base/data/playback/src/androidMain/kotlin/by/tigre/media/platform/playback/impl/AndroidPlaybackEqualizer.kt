package by.tigre.media.platform.playback.impl

import android.media.audiofx.Equalizer
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import by.tigre.logger.Log
import by.tigre.media.platform.playback.AndroidPlaybackPlayer
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.eq.EqGainInterpolation
import by.tigre.media.platform.playback.eq.EqPresetSource
import by.tigre.media.platform.playback.eq.UiEqConfig
import by.tigre.media.platform.playback.prefs.CustomEqPresetBank
import by.tigre.media.platform.playback.prefs.EqualizerPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

internal class AndroidPlaybackEqualizer(
    private val androidPlaybackPlayer: AndroidPlaybackPlayer,
    private val equalizerPrefs: EqualizerPreferences,
    private val uiEqConfig: UiEqConfig = UiEqConfig.musicDefault(),
) : PlaybackEqualizer {

    private val uiBandCentersHz: FloatArray = uiEqConfig.bandCentersHz
    private val customBank = CustomEqPresetBank(
        prefs = equalizerPrefs,
        bandCount = { uiBandCentersHz.size },
    )

    private val _available = MutableStateFlow(false)
    override val isAvailable: StateFlow<Boolean> = _available.asStateFlow()

    private val _presetNames = MutableStateFlow<List<String>>(emptyList())
    override val presetNames: StateFlow<List<String>> = _presetNames.asStateFlow()

    private val _selected = MutableStateFlow(0)
    override val selectedPresetIndex: StateFlow<Int> = _selected.asStateFlow()

    private val _bandCenterHz = MutableStateFlow<List<Float>>(emptyList())
    override val bandCenterHz: StateFlow<List<Float>> = _bandCenterHz.asStateFlow()

    private val _bandGainDb = MutableStateFlow<List<Float>>(emptyList())
    override val bandGainDb: StateFlow<List<Float>> = _bandGainDb.asStateFlow()

    private val _builtInPresetBandGainsDb = MutableStateFlow<List<List<Float>>>(emptyList())
    override val builtInPresetBandGainsDb: StateFlow<List<List<Float>>> = _builtInPresetBandGainsDb.asStateFlow()

    private val _customPresetIndex = MutableStateFlow(-1)
    override val customPresetIndex: StateFlow<Int> = _customPresetIndex.asStateFlow()

    private val _customPresetCount = MutableStateFlow(0)
    override val customPresetCount: StateFlow<Int> = _customPresetCount.asStateFlow()

    private val _bandGainRangeDb = MutableStateFlow(-12f to 12f)
    override val bandGainRangeDb: StateFlow<Pair<Float, Float>> = _bandGainRangeDb.asStateFlow()

    private var equalizer: Equalizer? = null
    private var factoryPresetCount: Int = 0
    private var customBandLevelsMb: ShortArray = ShortArray(0)
    private var hardwareBandCentersHz: FloatArray = FloatArray(0)

    private val exoPlayer: ExoPlayer
        get() = androidPlaybackPlayer.player as ExoPlayer

    private val listener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            attachForSession(audioSessionId)
        }
    }

    init {
        fun attachToPlayer() {
            exoPlayer.addListener(listener)
            val sid = exoPlayer.audioSessionId
            if (sid != C.AUDIO_SESSION_ID_UNSET) {
                attachForSession(sid)
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            attachToPlayer()
        } else {
            runBlocking(Dispatchers.Main) { attachToPlayer() }
        }
    }

    private fun attachForSession(audioSessionId: Int) {
        releaseEqualizer()
        hardwareBandCentersHz = FloatArray(0)
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) {
            clearUnavailable()
            return
        }
        try {
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = true
            when (val source = uiEqConfig.presetSource) {
                is EqPresetSource.HardwareFactory -> attachHardwareFactory(eq)
                is EqPresetSource.Defined -> attachDefinedPresets(eq, source)
            }
        } catch (e: Exception) {
            Log.w("PlaybackEqualizer") { "Equalizer unavailable: ${e.message}" }
            clearUnavailable()
        }
    }

    private fun attachHardwareFactory(eq: Equalizer) {
        val n = eq.numberOfPresets.toInt()
        if (n <= 0) {
            eq.release()
            clearUnavailable()
            return
        }
        if (!initHardwareBands(eq)) return

        equalizer = eq
        factoryPresetCount = n
        _builtInPresetBandGainsDb.value = emptyList()
        publishCustomMeta(firstCustom = n)
        _available.value = true

        val maxIdx = _presetNames.value.lastIndex
        val savedIndex = equalizerPrefs.loadSelectedPresetIndex(0).coerceIn(0, maxIdx.coerceAtLeast(0))
        applyAbsolutePreset(savedIndex, persistSelection = false)
        customBandLevelsMb = ShortArray(eq.numberOfBands.toInt()) { b -> eq.getBandLevel(b.toShort()) }
    }

    private fun attachDefinedPresets(eq: Equalizer, source: EqPresetSource.Defined) {
        if (!initHardwareBands(eq)) return

        equalizer = eq
        factoryPresetCount = source.presets.size
        _builtInPresetBandGainsDb.value = source.presets.map { it.gainsDb.toList() }
        publishCustomMeta(firstCustom = source.presets.size)
        _available.value = true

        val maxIdx = _presetNames.value.lastIndex
        val savedIndex = equalizerPrefs.loadSelectedPresetIndex(0).coerceIn(0, maxIdx.coerceAtLeast(0))
        applyAbsolutePreset(savedIndex, persistSelection = false)
        customBandLevelsMb = ShortArray(eq.numberOfBands.toInt()) { b -> eq.getBandLevel(b.toShort()) }
    }

    private fun publishCustomMeta(firstCustom: Int) {
        val builtInNames = when (val source = uiEqConfig.presetSource) {
            is EqPresetSource.HardwareFactory ->
                List(factoryPresetCount) { i ->
                    equalizer?.getPresetName(i.toShort()) ?: "Preset $i"
                }
            is EqPresetSource.Defined -> source.presets.map { it.id }
        }
        _customPresetIndex.value = firstCustom
        _customPresetCount.value = customBank.count
        _presetNames.value = builtInNames + customBank.titles
    }

    private fun initHardwareBands(eq: Equalizer): Boolean {
        val bandCount = eq.numberOfBands.toInt()
        if (bandCount <= 0) {
            eq.release()
            clearUnavailable()
            return false
        }
        hardwareBandCentersHz = FloatArray(bandCount) { b ->
            eq.getCenterFreq(b.toShort()) / 1000f
        }
        val range = eq.bandLevelRange
        _bandGainRangeDb.value = range[0] / 100f to range[1] / 100f
        _bandCenterHz.value = uiBandCentersHz.toList()
        return true
    }

    private fun clearUnavailable() {
        _available.value = false
        _presetNames.value = emptyList()
        _bandCenterHz.value = emptyList()
        _bandGainDb.value = emptyList()
        _builtInPresetBandGainsDb.value = emptyList()
        _customPresetIndex.value = -1
        _customPresetCount.value = 0
        hardwareBandCentersHz = FloatArray(0)
        factoryPresetCount = 0
    }

    private fun readHardwareGains(eq: Equalizer, bandCount: Int): List<Float> =
        List(bandCount) { b -> eq.getBandLevel(b.toShort()) / 100f }

    private fun dbToMb(gainDb: Float, minMb: Int, maxMb: Int): Short =
        (gainDb * 100f).toInt().coerceIn(minMb, maxMb).toShort()

    private fun releaseEqualizer() {
        equalizer?.release()
        equalizer = null
    }

    override fun selectPreset(index: Int) {
        if (equalizer == null) return
        if (index !in _presetNames.value.indices) return
        applyAbsolutePreset(index, persistSelection = true)
    }

    private fun applyAbsolutePreset(index: Int, persistSelection: Boolean) {
        val eq = equalizer ?: return
        val firstCustom = _customPresetIndex.value
        if (firstCustom < 0) return
        _selected.value = index
        if (persistSelection) equalizerPrefs.saveSelectedPresetIndex(index)
        try {
            when (val source = uiEqConfig.presetSource) {
                is EqPresetSource.HardwareFactory -> {
                    if (index < factoryPresetCount) {
                        eq.usePreset(index.toShort())
                        val bc = eq.numberOfBands.toInt()
                        val hwGains = readHardwareGains(eq, bc)
                        _bandGainDb.value = expandHardwareGainsToUi(hwGains)
                        customBandLevelsMb = ShortArray(bc) { b -> eq.getBandLevel(b.toShort()) }
                    } else {
                        applyCustomSlot(eq, index - firstCustom)
                    }
                }
                is EqPresetSource.Defined -> {
                    if (index < firstCustom) {
                        val gains = source.presets[index].gainsDb.toList()
                        applyHardwareFromUiGains(gains)
                        _bandGainDb.value = gains
                        val bc = eq.numberOfBands.toInt()
                        customBandLevelsMb = ShortArray(bc) { b -> eq.getBandLevel(b.toShort()) }
                    } else {
                        applyCustomSlot(eq, index - firstCustom)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("PlaybackEqualizer") { "selectPreset failed: ${e.message}" }
        }
    }

    private fun applyCustomSlot(eq: Equalizer, slot: Int) {
        val uiGains = customBank.gains(slot)
        applyHardwareFromUiGains(uiGains)
        _bandGainDb.value = uiGains
        val bc = eq.numberOfBands.toInt()
        customBandLevelsMb = ShortArray(bc) { b -> eq.getBandLevel(b.toShort()) }
    }

    override fun setBandGainDb(bandIndex: Int, gainDb: Float) {
        val eq = equalizer ?: return
        val firstCustom = _customPresetIndex.value
        if (firstCustom < 0) return
        if (bandIndex !in uiBandCentersHz.indices) return

        val range = _bandGainRangeDb.value
        val clamped = gainDb.coerceIn(range.first, range.second)
        val current = _bandGainDb.value.toMutableList()
        while (current.size < uiBandCentersHz.size) current.add(0f)
        current[bandIndex] = clamped

        val selected = _selected.value
        val targetAbsolute = if (customBank.isCustomAbsolute(selected, firstCustom)) {
            selected
        } else {
            firstCustom
        }
        val slot = targetAbsolute - firstCustom

        try {
            applyHardwareFromUiGains(current)
            _bandGainDb.value = current.toList()
            _selected.value = targetAbsolute
            equalizerPrefs.saveSelectedPresetIndex(targetAbsolute)
            customBank.updateGains(slot, current)
            publishCustomMeta(firstCustom)
        } catch (e: Exception) {
            Log.w("PlaybackEqualizer") { "setBandLevel failed: ${e.message}" }
        }
    }

    override fun addCustomPreset(): Boolean {
        val firstCustom = _customPresetIndex.value
        if (firstCustom < 0) return false
        val slot = customBank.addCopy(_bandGainDb.value)
        if (slot < 0) return false
        publishCustomMeta(firstCustom)
        val absolute = firstCustom + slot
        applyAbsolutePreset(absolute, persistSelection = true)
        return true
    }

    override fun renameCustomPreset(presetIndex: Int, title: String): Boolean {
        val firstCustom = _customPresetIndex.value
        if (!customBank.isCustomAbsolute(presetIndex, firstCustom)) return false
        val ok = customBank.rename(presetIndex - firstCustom, title)
        if (ok) publishCustomMeta(firstCustom)
        return ok
    }

    private fun applyHardwareFromUiGains(uiGains: List<Float>) {
        val eq = equalizer ?: return
        val bc = eq.numberOfBands.toInt()
        val collapsed = collapseUiGainsToHardware(uiGains)
        if (collapsed.size != bc) return
        val range = eq.bandLevelRange
        val minMb = range[0].toInt()
        val maxMb = range[1].toInt()
        for (b in 0 until bc) {
            eq.setBandLevel(b.toShort(), dbToMb(collapsed[b], minMb, maxMb))
        }
        if (customBandLevelsMb.size == bc) {
            customBandLevelsMb = ShortArray(bc) { b -> eq.getBandLevel(b.toShort()) }
        }
    }

    private fun expandHardwareGainsToUi(hwGains: List<Float>): List<Float> {
        val hwC = hardwareBandCentersHz
        if (hwGains.size != hwC.size) return List(uiBandCentersHz.size) { 0f }
        val hwG = hwGains.toFloatArray()
        return List(uiBandCentersHz.size) { i ->
            EqGainInterpolation.interpolateGainDb(uiBandCentersHz[i].toDouble(), hwC, hwG)
        }
    }

    private fun collapseUiGainsToHardware(uiGains: List<Float>): List<Float> {
        val hwC = hardwareBandCentersHz
        val padded = uiGains.toMutableList()
        while (padded.size < uiBandCentersHz.size) padded.add(0f)
        val uiG = padded.take(uiBandCentersHz.size).toFloatArray()
        return List(hwC.size) { j ->
            EqGainInterpolation.interpolateGainDb(hwC[j].toDouble(), uiBandCentersHz, uiG)
        }
    }
}
