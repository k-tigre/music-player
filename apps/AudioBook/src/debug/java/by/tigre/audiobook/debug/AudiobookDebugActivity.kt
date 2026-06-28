package by.tigre.audiobook.debug

import by.tigre.audiobook.App
import by.tigre.audiobook.debug.nighttimer.NightTimerShakeDebugComponent
import by.tigre.debug_settings.DebugActivity
import by.tigre.debug_settings.DebugPageComponent
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.presentation.appChildContext

class AudiobookDebugActivity : DebugActivity() {

    override fun createExtraPages(componentContext: BaseComponentContext): List<DebugPageComponent> {
        val nightTimer = (application as App).graph.nightTimerController
        return listOf(
            NightTimerShakeDebugComponent.Impl(
                componentContext.appChildContext("nightTimerShake"),
                nightTimer,
            ),
        )
    }
}
