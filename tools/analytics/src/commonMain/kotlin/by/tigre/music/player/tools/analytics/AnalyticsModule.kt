package by.tigre.music.player.tools.analytics

import by.tigre.music.player.tools.coroutines.CoreDispatchers
import by.tigre.music.player.tools.coroutines.CoreScope
import by.tigre.music.player.tools.coroutines.CoroutineModule

interface AnalyticsModule {
    val eventAnalytics: EventAnalytics
    val screenAnalytics: ScreenAnalytics

    class Impl(
        tracker: Tracker,
        coroutineModule: CoroutineModule,
        dispatchers: CoreDispatchers = CoreDispatchers.Impl(),
    ) : AnalyticsModule {
        override val eventAnalytics: EventAnalytics by lazy { EventAnalytics.Impl(tracker) }

        override val screenAnalytics: ScreenAnalytics by lazy {
            ScreenAnalytics.Impl(
                tracker = tracker,
                dispatchers = dispatchers,
                scope = coroutineModule.scope,
            )
        }
    }
}
