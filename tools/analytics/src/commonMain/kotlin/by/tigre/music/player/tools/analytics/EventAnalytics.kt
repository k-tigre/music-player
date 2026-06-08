package by.tigre.music.player.tools.analytics

interface EventAnalytics {
    fun trackEvent(event: Event.Action)

    class Impl(
        private val tracker: Tracker,
    ) : EventAnalytics {
        override fun trackEvent(event: Event.Action) = tracker.trackEvent(event)
    }
}
