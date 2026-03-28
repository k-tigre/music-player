package by.tigre.music.player.core.data.playback.impl

import android.media.audiofx.Equalizer
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import by.tigre.music.player.core.data.playback.AndroidPlaybackPlayer
import by.tigre.music.player.core.data.playback.PlaybackEqualizer
import by.tigre.music.player.logger.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class AndroidPlaybackEqualizer(
    private val androidPlaybackPlayer: AndroidPlaybackPlayer,
) : PlaybackEqualizer {

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

            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()
            _bandGainRangeDb.value = minMb / 100f to maxMb / 100f

            _bandCenterHz.value =
                List(bandCount) { b ->
                    eq.getCenterFreq(b.toShort()) / 1000f
                }

            _presetNames.value = List(n) { i -> eq.getPresetName(i.toShort()) } + "Custom"
            _customPresetIndex.value = n

            _available.value = true
            val customUiIdx = n
            val idx = _selected.value.coerceIn(0, customUiIdx)
            _selected.value = idx
            if (idx < n) {
                eq.usePreset(idx.toShort())
            } else {
                eq.usePreset(0.toShort())
                val flat = 0.toShort()
                for (b in 0 until bandCount) {
                    eq.setBandLevel(b.toShort(), flat)
                }
            }
            customBandLevelsMb = ShortArray(bandCount) { b -> eq.getBandLevel(b.toShort()) }
            readBandsToState(eq, bandCount)
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
    }

    private fun readBandsToState(eq: Equalizer, bandCount: Int) {
        _bandGainDb.value =
            List(bandCount) { b ->
                eq.getBandLevel(b.toShort()) / 100f
            }
    }

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
                readBandsToState(eq, bc)
                customBandLevelsMb = ShortArray(bc) { b -> eq.getBandLevel(b.toShort()) }
            } else if (index == customIdx) {
                val bc = eq.numberOfBands.toInt()
                for (b in 0 until bc) {
                    eq.setBandLevel(b.toShort(), customBandLevelsMb[b])
                }
                readBandsToState(eq, bc)
            }
        } catch (e: Exception) {
            Log.w("PlaybackEqualizer") { "selectPreset failed: ${e.message}" }
        }
    }

    override fun setBandGainDb(bandIndex: Int, gainDb: Float) {
        val eq = equalizer ?: return
        val customIdx = _customPresetIndex.value
        if (customIdx < 0) return
        val bc = eq.numberOfBands.toInt()
        if (bandIndex !in 0 until bc) return

        val range = eq.bandLevelRange
        val minMb = range[0].toInt()
        val maxMb = range[1].toInt()
        val mb = (gainDb * 100f).toInt().coerceIn(minMb, maxMb).toShort()

        try {
            eq.setBandLevel(bandIndex.toShort(), mb)
            if (customBandLevelsMb.size == bc) {
                customBandLevelsMb[bandIndex] = mb
            }
            _selected.value = customIdx
            readBandsToState(eq, bc)
        } catch (e: Exception) {
            Log.w("PlaybackEqualizer") { "setBandLevel failed: ${e.message}" }
        }
    }
}
