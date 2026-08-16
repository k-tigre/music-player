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

    private val _bandGainRangeDb = MutableStateFlow(-12f to 12f)
    override val bandGainRangeDb: StateFlow<Pair<Float, Float>> = _bandGainRangeDb.asStateFlow()

    private var equalizer: Equalizer? = null
    private var factoryPresetCount: Int = 0
    private var customBandLevelsMb: ShortArray = ShortArray(0)

    /** Device graphic EQ band centers (Hz), length = [Equalizer.getNumberOfBands]. */
    private var hardwareBandCentersHz: FloatArray = FloatArray(0)

    private val exoPlayer: ExoPlayer
        get() = androidPlaybackPlayer.player as ExoPlayer

    private val listener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            attachForSession(audioSessionId)
        }
    }

    init {
        // ExoPlayer session APIs require the application thread; construction may happen off-main.
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
        _presetNames.value = List(n) { i -> eq.getPresetName(i.toShort()) } + "Custom"
        _customPresetIndex.value = n
        _available.value = true

        val customUiIdx = n
        val savedIndex = equalizerPrefs.loadSelectedPresetIndex(0).coerceIn(0, customUiIdx)
        _selected.value = savedIndex

        if (savedIndex < n) {
            eq.usePreset(savedIndex.toShort())
            val hwGains = readHardwareGains(eq, eq.numberOfBands.toInt())
            _bandGainDb.value = expandHardwareGainsToUi(hwGains)
        } else {
            val uiGains = loadAlignedCustomGains()
            applyHardwareFromUiGains(uiGains)
            _bandGainDb.value = uiGains
        }
        customBandLevelsMb = ShortArray(eq.numberOfBands.toInt()) { b -> eq.getBandLevel(b.toShort()) }
    }

    private fun attachDefinedPresets(eq: Equalizer, source: EqPresetSource.Defined) {
        if (!initHardwareBands(eq)) return

        equalizer = eq
        factoryPresetCount = source.presets.size
        _builtInPresetBandGainsDb.value = source.presets.map { it.gainsDb.toList() }
        _presetNames.value = source.presets.map { it.id } + "Custom"
        _customPresetIndex.value = source.presets.size
        _available.value = true

        val customUiIdx = source.presets.size
        val savedIndex = equalizerPrefs.loadSelectedPresetIndex(0).coerceIn(0, customUiIdx)
        _selected.value = savedIndex

        if (savedIndex < customUiIdx) {
            val gains = source.presets[savedIndex].gainsDb.toList()
            applyHardwareFromUiGains(gains)
            _bandGainDb.value = gains
        } else {
            val uiGains = loadAlignedCustomGains()
            applyHardwareFromUiGains(uiGains)
            _bandGainDb.value = uiGains
        }
        customBandLevelsMb = ShortArray(eq.numberOfBands.toInt()) { b -> eq.getBandLevel(b.toShort()) }
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

    private fun loadAlignedCustomGains(): List<Float> =
        EqGainInterpolation.alignOrRemapToUiBands(
            equalizerPrefs.loadCustomBandGainsDb(),
            uiBandCentersHz,
        )

    private fun clearUnavailable() {
        _available.value = false
        _presetNames.value = emptyList()
        _bandCenterHz.value = emptyList()
        _bandGainDb.value = emptyList()
        _builtInPresetBandGainsDb.value = emptyList()
        _customPresetIndex.value = -1
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
        val eq = equalizer ?: return
        val names = _presetNames.value
        if (index !in names.indices) return
        val customIdx = _customPresetIndex.value
        if (customIdx < 0) return

        _selected.value = index
        try {
            when (val source = uiEqConfig.presetSource) {
                is EqPresetSource.HardwareFactory -> selectHardwarePreset(eq, index, customIdx)
                is EqPresetSource.Defined -> selectDefinedPreset(eq, source, index, customIdx)
            }
        } catch (e: Exception) {
            Log.w("PlaybackEqualizer") { "selectPreset failed: ${e.message}" }
        }
    }

    private fun selectHardwarePreset(eq: Equalizer, index: Int, customIdx: Int) {
        val n = factoryPresetCount
        if (index < n) {
            eq.usePreset(index.toShort())
            val bc = eq.numberOfBands.toInt()
            val hwGains = readHardwareGains(eq, bc)
            _bandGainDb.value = expandHardwareGainsToUi(hwGains)
            customBandLevelsMb = ShortArray(bc) { b -> eq.getBandLevel(b.toShort()) }
            equalizerPrefs.saveSelectedPresetIndex(index)
        } else if (index == customIdx) {
            val uiGains = loadAlignedCustomGains()
            applyHardwareFromUiGains(uiGains)
            _bandGainDb.value = uiGains
            val bc = eq.numberOfBands.toInt()
            customBandLevelsMb = ShortArray(bc) { b -> eq.getBandLevel(b.toShort()) }
            equalizerPrefs.saveSelectedPresetIndex(customIdx)
            equalizerPrefs.saveCustomBandGainsDb(uiGains)
        }
    }

    private fun selectDefinedPreset(
        eq: Equalizer,
        source: EqPresetSource.Defined,
        index: Int,
        customIdx: Int,
    ) {
        if (index < customIdx) {
            val gains = source.presets[index].gainsDb.toList()
            applyHardwareFromUiGains(gains)
            _bandGainDb.value = gains
            val bc = eq.numberOfBands.toInt()
            customBandLevelsMb = ShortArray(bc) { b -> eq.getBandLevel(b.toShort()) }
            equalizerPrefs.saveSelectedPresetIndex(index)
        } else if (index == customIdx) {
            val uiGains = loadAlignedCustomGains()
            applyHardwareFromUiGains(uiGains)
            _bandGainDb.value = uiGains
            val bc = eq.numberOfBands.toInt()
            customBandLevelsMb = ShortArray(bc) { b -> eq.getBandLevel(b.toShort()) }
            equalizerPrefs.saveSelectedPresetIndex(customIdx)
            equalizerPrefs.saveCustomBandGainsDb(uiGains)
        }
    }

    override fun setBandGainDb(bandIndex: Int, gainDb: Float) {
        val eq = equalizer ?: return
        val customIdx = _customPresetIndex.value
        if (customIdx < 0) return
        if (bandIndex !in uiBandCentersHz.indices) return

        val range = _bandGainRangeDb.value
        val clamped = gainDb.coerceIn(range.first, range.second)
        val current = _bandGainDb.value.toMutableList()
        while (current.size < uiBandCentersHz.size) current.add(0f)
        current[bandIndex] = clamped

        try {
            applyHardwareFromUiGains(current)
            _bandGainDb.value = current.toList()
            _selected.value = customIdx
            equalizerPrefs.saveSelectedPresetIndex(customIdx)
            equalizerPrefs.saveCustomBandGainsDb(current)
        } catch (e: Exception) {
            Log.w("PlaybackEqualizer") { "setBandLevel failed: ${e.message}" }
        }
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
