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

    private var equalizer: Equalizer? = null

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
            _available.value = false
            _presetNames.value = emptyList()
            return
        }
        try {
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = true
            val n = eq.numberOfPresets.toInt()
            if (n <= 0) {
                eq.release()
                _available.value = false
                _presetNames.value = emptyList()
                return
            }
            equalizer = eq
            _presetNames.value = List(n) { i -> eq.getPresetName(i.toShort()) }
            _available.value = true
            val idx = _selected.value.coerceIn(0, n - 1)
            _selected.value = idx
            eq.usePreset(idx.toShort())
        } catch (e: Exception) {
            Log.w("PlaybackEqualizer") { "Equalizer unavailable: ${e.message}" }
            _available.value = false
            _presetNames.value = emptyList()
        }
    }

    private fun releaseEqualizer() {
        equalizer?.release()
        equalizer = null
    }

    override fun selectPreset(index: Int) {
        _selected.value = index
        val eq = equalizer ?: return
        val n = eq.numberOfPresets.toInt()
        if (n <= 0) return
        val idx = index.coerceIn(0, n - 1)
        try {
            eq.usePreset(idx.toShort())
        } catch (e: Exception) {
            Log.w("PlaybackEqualizer") { "usePreset failed: ${e.message}" }
        }
    }
}
