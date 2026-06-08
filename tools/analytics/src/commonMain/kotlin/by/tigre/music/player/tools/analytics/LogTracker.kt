package by.tigre.music.player.tools.analytics

import by.tigre.logger.Log

class LogTracker : Tracker {
    override fun trackEvent(event: Event.Action) {
        Log.i("Analytics:Event") { "${event.name}${event.payloadString()}" }
    }

    override fun trackScreen(previous: Event.Screen?, current: Event.Screen) {
        Log.i("Analytics:Screen") { "${current.name}${current.payloadString()}, previous:${previous?.name}" }
    }

    private fun Event.payloadString() = if (this is Event.WithPayload) ": $payload" else ""
}
