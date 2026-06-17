package by.tigre.media.platform.tools.analytics.common

interface CommonEventAnalytics {
    fun trackEvent(event: CommonEvents.Action)
}

interface CommonScreenAnalytics {
    fun trackScreen(screen: CommonEvents.Screen)
}

interface CommonAnalyticsDependency {
    val eventAnalytics: CommonEventAnalytics
    val screenAnalytics: CommonScreenAnalytics
}
