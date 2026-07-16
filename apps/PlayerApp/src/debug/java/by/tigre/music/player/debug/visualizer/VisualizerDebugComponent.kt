package by.tigre.music.player.debug.visualizer

import by.tigre.debug_settings.ComposableDebugPage
import by.tigre.media.platform.playback.VisualizerMode
import by.tigre.media.platform.playback.VisualizerProcessing
import by.tigre.media.platform.playback.prefs.VisualizerPreferences
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.tools.platform.compose.ComposableView
import kotlinx.coroutines.flow.StateFlow

internal interface VisualizerDebugComponent : ComposableDebugPage {
    val mode: StateFlow<VisualizerMode>
    val processing: StateFlow<VisualizerProcessing>
    fun setMode(mode: VisualizerMode)
    fun setProcessing(processing: VisualizerProcessing)

    class Impl(
        componentContext: BaseComponentContext,
        private val preferences: VisualizerPreferences,
    ) : VisualizerDebugComponent, BaseComponentContext by componentContext {

        override val title: String = "Visualizer"
        override val view: ComposableView = VisualizerDebugView(this)
        override val mode: StateFlow<VisualizerMode> = preferences.mode
        override val processing: StateFlow<VisualizerProcessing> = preferences.processing

        override fun setMode(mode: VisualizerMode) {
            preferences.setMode(mode)
        }

        override fun setProcessing(processing: VisualizerProcessing) {
            preferences.setProcessing(processing)
        }
    }
}
