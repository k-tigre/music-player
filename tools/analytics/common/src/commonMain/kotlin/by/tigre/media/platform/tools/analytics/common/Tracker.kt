package by.tigre.media.platform.tools.analytics.common

interface Tracker {
    fun trackAction(event: AnalyticsAction)
    fun trackScreen(previous: AnalyticsScreen?, current: AnalyticsScreen)

    class Aggregator(private vararg val trackers: Tracker) : Tracker {
        override fun trackAction(event: AnalyticsAction) {
            trackers.forEach { it.trackAction(event) }
        }

        override fun trackScreen(previous: AnalyticsScreen?, current: AnalyticsScreen) {
            trackers.forEach { it.trackScreen(previous, current) }
        }
    }
}
