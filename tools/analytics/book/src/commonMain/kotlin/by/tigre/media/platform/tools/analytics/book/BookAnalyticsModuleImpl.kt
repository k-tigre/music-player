package by.tigre.media.platform.tools.analytics.book

import by.tigre.media.platform.tools.analytics.common.Tracker
import by.tigre.media.platform.tools.coroutines.CoreDispatchers
import by.tigre.media.platform.tools.coroutines.CoroutineModule

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
