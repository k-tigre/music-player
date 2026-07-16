package by.tigre.media.platform.playback.prefs

import by.tigre.media.platform.playback.VisualizerMode
import by.tigre.media.platform.playback.VisualizerProcessing
import by.tigre.media.platform.preferences.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VisualizerPreferences(
    private val preferences: Preferences,
) {
    private val _mode = MutableStateFlow(loadMode())
    val mode: StateFlow<VisualizerMode> = _mode.asStateFlow()

    private val _processing = MutableStateFlow(loadProcessing())
    val processing: StateFlow<VisualizerProcessing> = _processing.asStateFlow()

    fun setMode(mode: VisualizerMode) {
        preferences.saveString(KEY_MODE, mode.name)
        _mode.value = mode
    }

    fun setProcessing(processing: VisualizerProcessing) {
        preferences.saveString(KEY_PROCESSING, processing.name)
        _processing.value = processing
    }

    private fun loadMode(): VisualizerMode =
        VisualizerMode.fromStorage(preferences.loadString(KEY_MODE, VisualizerMode.Off.name))

    private fun loadProcessing(): VisualizerProcessing =
        VisualizerProcessing.fromStorage(
            preferences.loadString(KEY_PROCESSING, VisualizerProcessing.Pretty.name),
        )

    companion object {
        const val KEY_MODE = "player_visualizer_mode"
        const val KEY_PROCESSING = "player_visualizer_processing"
    }
}
