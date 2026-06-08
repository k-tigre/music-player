package by.tigre.music.player.tools.analytics.book

import by.tigre.music.player.tools.analytics.common.Tracker
import by.tigre.music.player.tools.coroutines.CoreDispatchers
import by.tigre.music.player.tools.coroutines.CoroutineModule

class BookAnalyticsModuleImpl private constructor(
    override val eventAnalytics: BookEventAnalytics,
    override val screenAnalytics: BookScreenAnalytics,
) : BookAnalyticsModule {

    companion object {
        fun create(
            tracker: Tracker,
            coroutineModule: CoroutineModule,
            dispatchers: CoreDispatchers = CoreDispatchers.Impl(),
        ): BookAnalyticsModule = BookAnalyticsModuleImpl(
            eventAnalytics = BookEventAnalyticsImpl(tracker),
            screenAnalytics = BookScreenAnalyticsImpl(
                tracker = tracker,
                dispatchers = dispatchers,
                scope = coroutineModule.scope,
            ),
        )
    }
}
