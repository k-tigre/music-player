package by.tigre.music.player.tools.analytics

import by.tigre.logger.Log
import by.tigre.music.player.tools.analytics.common.AnalyticsAction
import by.tigre.music.player.tools.analytics.common.AnalyticsScreen
import by.tigre.music.player.tools.analytics.common.Tracker
import by.tigre.music.player.tools.analytics.common.WithPayload

class LogTracker : Tracker {
    override fun trackAction(event: AnalyticsAction) {
        Log.d(TAG) { "ACTION ${event.name}${event.payloadSuffix()}" }
    }

    override fun trackScreen(previous: AnalyticsScreen?, current: AnalyticsScreen) {
        Log.d(TAG) { "SCREEN ${current.name} (prev=${previous?.name})${current.payloadSuffix()}" }
    }

    private fun AnalyticsAction.payloadSuffix(): String =
        (this as? WithPayload)?.payload?.entries?.joinToString(prefix = " ", separator = ", ") { "${it.key}=${it.value}" }
            .orEmpty()

    private fun AnalyticsScreen.payloadSuffix(): String =
        (this as? WithPayload)?.payload?.entries?.joinToString(prefix = " ", separator = ", ") { "${it.key}=${it.value}" }
            .orEmpty()

    private companion object {
        const val TAG = "Analytics"
    }
}
