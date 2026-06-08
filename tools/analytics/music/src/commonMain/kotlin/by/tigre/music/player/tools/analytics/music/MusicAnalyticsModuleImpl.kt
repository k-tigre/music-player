package by.tigre.music.player.tools.analytics.music

import by.tigre.music.player.tools.analytics.common.Tracker
import by.tigre.music.player.tools.coroutines.CoreDispatchers
import by.tigre.music.player.tools.coroutines.CoroutineModule

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
