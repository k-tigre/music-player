package by.tigre.media.platform.tools.analytics.book

import by.tigre.media.platform.tools.analytics.common.CommonEvents
import by.tigre.media.platform.tools.analytics.common.ScreenAnalyticsEngine
import by.tigre.media.platform.tools.analytics.common.Tracker
import by.tigre.media.platform.tools.coroutines.CoreDispatchers
import by.tigre.media.platform.tools.coroutines.CoreScope

internal class BookScreenAnalyticsImpl(
    tracker: Tracker,
    dispatchers: CoreDispatchers,
    scope: CoreScope,
) : BookScreenAnalytics {
    private val engine = ScreenAnalyticsEngine(tracker, dispatchers, scope)

    override fun trackScreen(screen: CommonEvents.Screen) = engine.trackScreen(screen)
    override fun trackScreen(screen: AudiobookEvents.Screen) = engine.trackScreen(screen)
}
