package by.tigre.music.player.presentation.base

import by.tigre.logger.Log
import by.tigre.logger.extensions.debugLog
import by.tigre.music.player.tools.analytics.Event
import by.tigre.music.player.tools.analytics.ScreenAnalytics
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.mapNotNull

const val TAG_UNEXPECTED = "UNEXPECTED"

suspend inline fun <reified T> Value<ChildStack<*, *>>.trackScreens(
    analytics: ScreenAnalytics,
    name: String,
    crossinline screenMapper: (T) -> Event.Screen,
) {
    toFlow()
        .debugLog("trackScreens", name)
        .mapNotNull {
            (it.active.configuration as? T)?.let(screenMapper)
                ?: run {
                    Log.e(TAG_UNEXPECTED) { "handleUntrackedScreenConfig: ${it.active.configuration::class.java.name}" }
                    null
                }
        }
        .collect(analytics::trackScreen)
}
