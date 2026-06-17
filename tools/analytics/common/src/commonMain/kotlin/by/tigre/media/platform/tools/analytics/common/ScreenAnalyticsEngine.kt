package by.tigre.media.platform.tools.analytics.common

import by.tigre.logger.extensions.debugLog
import by.tigre.media.platform.tools.coroutines.CoreDispatchers
import by.tigre.media.platform.tools.coroutines.CoreScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch

class ScreenAnalyticsEngine(
    private val tracker: Tracker,
    dispatchers: CoreDispatchers,
    scope: CoreScope,
) {
    private val screens = MutableSharedFlow<AnalyticsScreen>()
    private val coreScope = scope

    init {
        scope.launch {
            screens
                .debugLog("ScreenAnalytics", "trackScreens")
                .distinctUntilChanged()
                .scan((null to null) as Pair<AnalyticsScreen?, AnalyticsScreen?>) { previous, current ->
                    if (current.skip) previous else previous.second to current
                }
                .drop(1)
                .distinctUntilChanged()
                .flowOn(dispatchers.io)
                .collect { (prev, current) ->
                    if (current != null) {
                        tracker.trackScreen(previous = prev, current = current)
                    }
                }
        }
    }

    fun trackScreen(screen: AnalyticsScreen) {
        coreScope.launch { screens.emit(screen) }
    }
}
