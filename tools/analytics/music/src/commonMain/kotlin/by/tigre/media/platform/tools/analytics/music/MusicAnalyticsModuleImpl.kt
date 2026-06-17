package by.tigre.media.platform.tools.analytics.music

import by.tigre.media.platform.tools.analytics.common.Tracker
import by.tigre.media.platform.tools.coroutines.CoreDispatchers
import by.tigre.media.platform.tools.coroutines.CoroutineModule

class MusicAnalyticsModuleImpl private constructor(
    override val eventAnalytics: MusicEventAnalytics,
    override val screenAnalytics: MusicScreenAnalytics,
) : MusicAnalyticsModule {

    companion object {
        fun create(
            tracker: Tracker,
            coroutineModule: CoroutineModule,
            dispatchers: CoreDispatchers = CoreDispatchers.Impl(),
        ): MusicAnalyticsModule = MusicAnalyticsModuleImpl(
            eventAnalytics = MusicEventAnalyticsImpl(tracker),
            screenAnalytics = MusicScreenAnalyticsImpl(
                tracker = tracker,
                dispatchers = dispatchers,
                scope = coroutineModule.scope,
            ),
        )
    }
}
