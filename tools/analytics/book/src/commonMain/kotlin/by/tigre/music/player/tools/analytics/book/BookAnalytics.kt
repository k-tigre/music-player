package by.tigre.music.player.tools.analytics.book

import by.tigre.music.player.tools.analytics.common.CommonAnalyticsDependency
import by.tigre.music.player.tools.analytics.common.CommonEvents
import by.tigre.music.player.tools.analytics.common.CommonEventAnalytics
import by.tigre.music.player.tools.analytics.common.CommonScreenAnalytics

interface BookEventAnalytics : CommonEventAnalytics {
    override fun trackEvent(event: CommonEvents.Action)
    fun trackEvent(event: AudiobookEvents.Action)
}

interface BookScreenAnalytics : CommonScreenAnalytics {
    override fun trackScreen(screen: CommonEvents.Screen)
    fun trackScreen(screen: AudiobookEvents.Screen)
}

interface BookAnalyticsDependency : CommonAnalyticsDependency {
    override val eventAnalytics: BookEventAnalytics
    override val screenAnalytics: BookScreenAnalytics
}

interface BookAnalyticsModule : BookAnalyticsDependency
