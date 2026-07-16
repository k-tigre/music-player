package by.tigre.music.player.debug

import by.tigre.debug_settings.DebugActivity
import by.tigre.debug_settings.DebugPageComponent
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.presentation.appChildContext
import by.tigre.music.player.App
import by.tigre.music.player.debug.visualizer.VisualizerDebugComponent

class PlayerAppDebugActivity : DebugActivity() {

    override fun createExtraPages(componentContext: BaseComponentContext): List<DebugPageComponent> {
        val prefs = (application as App).graph.visualizerPreferences
        return listOf(
            VisualizerDebugComponent.Impl(
                componentContext.appChildContext("visualizer"),
                prefs,
            ),
        )
    }
}
