package by.tigre.music.player.tools.analytics.music

import by.tigre.music.player.tools.analytics.common.CommonEvents
import by.tigre.music.player.tools.analytics.common.ScreenAnalyticsEngine
import by.tigre.music.player.tools.analytics.common.Tracker
import by.tigre.media.platform.tools.coroutines.CoreDispatchers
import by.tigre.media.platform.tools.coroutines.CoreScope

internal class MusicScreenAnalyticsImpl(
    tracker: Tracker,
    dispatchers: CoreDispatchers,
    scope: CoreScope,
) : MusicScreenAnalytics {
    private val engine = ScreenAnalyticsEngine(tracker, dispatchers, scope)

    override fun trackScreen(screen: CommonEvents.Screen) = engine.trackScreen(screen)
    override fun trackScreen(screen: MusicEvents.Screen) = engine.trackScreen(screen)
}
