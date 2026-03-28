package by.tigre.music.player.core.data.playback.impl

import by.tigre.music.player.core.data.playback.PlaybackEqualizer
import by.tigre.music.player.core.data.playback.impl.dsp.DesktopEqualizerPresets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class DesktopPlaybackEqualizer(
    private val player: JdkClipDesktopPlaybackPlayer,
) : PlaybackEqualizer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _available = MutableStateFlow(true)
    override val isAvailable: StateFlow<Boolean> = _available.asStateFlow()

    private val _names = MutableStateFlow(DesktopEqualizerPresets.names)
    override val presetNames: StateFlow<List<String>> = _names.asStateFlow()

    private val _selected = MutableStateFlow(0)
    override val selectedPresetIndex: StateFlow<Int> = _selected.asStateFlow()

    override fun selectPreset(index: Int) {
        if (index !in _names.value.indices) return
        _selected.value = index
        scope.launch {
            player.applyEqualizerPreset(index)
        }
    }
}
