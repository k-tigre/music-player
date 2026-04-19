package by.tigre.music.player.core.data.playback.impl

import android.media.audiofx.Equalizer
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import by.tigre.music.player.core.data.playback.AndroidPlaybackPlayer
import by.tigre.music.player.core.data.playback.PlaybackEqualizer
import by.tigre.music.player.core.data.playback.prefs.EqualizerPreferences
import by.tigre.music.player.core.data.playback.prefs.alignGainsToBandCount
import by.tigre.music.player.logger.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.ln

internal class AndroidPlaybackEqualizer(
    private val androidPlaybackPlayer: AndroidPlaybackPlayer,
    private val equalizerPrefs: EqualizerPreferences,
) : PlaybackEqualizer {

    /**
     * UI band centers (Hz), mapped to/from device bands via log-frequency interpolation.
     * Must match [by.tigre.music.player.core.data.playback.impl.dsp.DesktopEqualizerPresets.bandCentersHz].
     */
    private val uiBandCentersHz = floatArrayOf(
        32f, 64f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f, 20000f,
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
        exoPlayer.addListener(listener)
        val sid = exoPlayer.audioSessionId
        if (sid != C.AUDIO_SESSION_ID_UNSET) {
            attachForSession(sid)
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
            val n = eq.numberOfPresets.toInt()
            if (n <= 0) {
                eq.release()
                clearUnavailable()
                return
            }
            equalizer = eq
            factoryPresetCount = n
            val bandCount = eq.numberOfBands.toInt()
            if (bandCount <= 0) {
                eq.release()
                equalizer = null
                clearUnavailable()
                return
            }

            hardwareBandCentersHz = FloatArray(bandCount) { b ->
                eq.getCenterFreq(b.toShort()) / 1000f
            }

            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()
            _bandGainRangeDb.value = minMb / 100f to maxMb / 100f

            _bandCenterHz.value = uiBandCentersHz.toList()

            _presetNames.value = List(n) { i -> eq.getPresetName(i.toShort()) } + "Custom"
            _customPresetIndex.value = n

            _available.value = true
            val customUiIdx = n
            val savedIndex = equalizerPrefs.loadSelectedPresetIndex(0).coerceIn(0, customUiIdx)
            _selected.value = savedIndex

            if (savedIndex < n) {
                eq.usePreset(savedIndex.toShort())
                val hwGains = readHardwareGains(eq, bandCount)
                _bandGainDb.value = expandHardwareGainsToUi(hwGains)
            } else {
                val ui8 = alignGainsToBandCount(equalizerPrefs.loadCustomBandGainsDb(), uiBandCentersHz.size)
                applyHardwareFromUiGains(ui8)
                _bandGainDb.value = ui8
            }
            customBandLevelsMb = ShortArray(bandCount) { b -> eq.getBandLevel(b.toShort()) }
        } catch (e: Exception) {
            Log.w("PlaybackEqualizer") { "Equalizer unavailable: ${e.message}" }
            clearUnavailable()
        }
    }

    private fun clearUnavailable() {
        _available.value = false
        _presetNames.value = emptyList()
        _bandCenterHz.value = emptyList()
        _bandGainDb.value = emptyList()
        _builtInPresetBandGainsDb.value = emptyList()
        _customPresetIndex.value = -1
        hardwareBandCentersHz = FloatArray(0)
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
        val n = factoryPresetCount
        val customIdx = _customPresetIndex.value
        if (customIdx < 0) return

        _selected.value = index
        try {
            if (index < n) {
                eq.usePreset(index.toShort())
                val bc = eq.numberOfBands.toInt()
                val hwGains = readHardwareGains(eq, bc)
                _bandGainDb.value = expandHardwareGainsToUi(hwGains)
                customBandLevelsMb = ShortArray(bc) { b -> eq.getBandLevel(b.toShort()) }
                equalizerPrefs.saveSelectedPresetIndex(index)
            } else if (index == customIdx) {
                val ui8 = alignGainsToBandCount(equalizerPrefs.loadCustomBandGainsDb(), uiBandCentersHz.size)
                applyHardwareFromUiGains(ui8)
                _bandGainDb.value = ui8
                val bc = eq.numberOfBands.toInt()
                customBandLevelsMb = ShortArray(bc) { b -> eq.getBandLevel(b.toShort()) }
                equalizerPrefs.saveSelectedPresetIndex(customIdx)
                equalizerPrefs.saveCustomBandGainsDb(ui8)
            }
        } catch (e: Exception) {
            Log.w("PlaybackEqualizer") { "selectPreset failed: ${e.message}" }
        }
    }

    override fun setBandGainDb(bandIndex: Int, gainDb: Float) {
        val eq = equalizer ?: return
        val customIdx = _customPresetIndex.value
        if (customIdx < 0) return
        if (bandIndex !in 0 until uiBandCentersHz.size) return

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
            interpolateGainDb(uiBandCentersHz[i].toDouble(), hwC, hwG)
        }
    }

    private fun collapseUiGainsToHardware(uiGains: List<Float>): List<Float> {
        val hwC = hardwareBandCentersHz
        val padded = uiGains.toMutableList()
        while (padded.size < uiBandCentersHz.size) padded.add(0f)
        val uiG = padded.take(uiBandCentersHz.size).toFloatArray()
        return List(hwC.size) { j ->
            interpolateGainDb(hwC[j].toDouble(), uiBandCentersHz, uiG)
        }
    }

    private fun interpolateGainDb(hz: Double, centersHz: FloatArray, gainsDb: FloatArray): Float {
        require(centersHz.size == gainsDb.size)
        val log = ln(hz)
        val logs = DoubleArray(centersHz.size) { ln(centersHz[it].toDouble()) }
        if (log <= logs[0]) return gainsDb[0]
        if (log >= logs[logs.lastIndex]) return gainsDb[gainsDb.lastIndex]
        for (i in 0 until logs.lastIndex) {
            if (log <= logs[i + 1]) {
                val t = ((log - logs[i]) / (logs[i + 1] - logs[i])).toFloat()
                return gainsDb[i] + t * (gainsDb[i + 1] - gainsDb[i])
            }
        }
        return gainsDb[gainsDb.lastIndex]
    }
}
