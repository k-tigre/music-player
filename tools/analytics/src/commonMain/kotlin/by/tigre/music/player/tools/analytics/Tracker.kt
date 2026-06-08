package by.tigre.music.player.tools.analytics

interface Tracker {
    fun trackEvent(event: Event.Action)
    fun trackScreen(previous: Event.Screen?, current: Event.Screen)

    class TrackerAggregator(private vararg val trackers: Tracker) : Tracker {
        override fun trackEvent(event: Event.Action) {
            trackers.forEach { it.trackEvent(event) }
        }

        override fun trackScreen(previous: Event.Screen?, current: Event.Screen) {
            trackers.forEach { it.trackScreen(previous, current) }
        }
    }
}
