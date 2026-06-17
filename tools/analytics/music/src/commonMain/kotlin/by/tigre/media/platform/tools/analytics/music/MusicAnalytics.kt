package by.tigre.media.platform.tools.analytics.music

import by.tigre.media.platform.tools.analytics.common.CommonAnalyticsDependency
import by.tigre.media.platform.tools.analytics.common.CommonEvents
import by.tigre.media.platform.tools.analytics.common.CommonEventAnalytics
import by.tigre.media.platform.tools.analytics.common.CommonScreenAnalytics

interface MusicEventAnalytics : CommonEventAnalytics {
    override fun trackEvent(event: CommonEvents.Action)
    fun trackEvent(event: MusicEvents.Action)
}

interface MusicScreenAnalytics : CommonScreenAnalytics {
    override fun trackScreen(screen: CommonEvents.Screen)
    fun trackScreen(screen: MusicEvents.Screen)
}

interface MusicAnalyticsDependency : CommonAnalyticsDependency {
    override val eventAnalytics: MusicEventAnalytics
    override val screenAnalytics: MusicScreenAnalytics
}

interface MusicAnalyticsModule : MusicAnalyticsDependency
