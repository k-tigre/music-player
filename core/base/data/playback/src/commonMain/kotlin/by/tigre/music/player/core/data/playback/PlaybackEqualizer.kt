package by.tigre.music.player.core.data.playback

import kotlinx.coroutines.flow.StateFlow

interface PlaybackEqualizer {
    val isAvailable: StateFlow<Boolean>
    val presetNames: StateFlow<List<String>>
    val selectedPresetIndex: StateFlow<Int>

    fun selectPreset(index: Int)
}
