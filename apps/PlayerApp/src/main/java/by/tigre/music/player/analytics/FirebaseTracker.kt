package by.tigre.music.player.analytics

import android.content.Context
import by.tigre.music.player.tools.analytics.common.AnalyticsAction
import by.tigre.music.player.tools.analytics.common.AnalyticsScreen
import by.tigre.music.player.tools.analytics.common.Tracker
import by.tigre.music.player.tools.analytics.common.WithPayload
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent

class FirebaseTracker(context: Context) : Tracker {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun trackAction(event: AnalyticsAction) {
        if (event is WithPayload) {
            firebaseAnalytics.logEvent("ACTION_${event.name}") {
                event.payload.forEach { (key, value) ->
                    param(key, value)
                }
            }
        } else {
            firebaseAnalytics.logEvent("ACTION_${event.name}", null)
        }
    }

    override fun trackScreen(previous: AnalyticsScreen?, current: AnalyticsScreen) {
        firebaseAnalytics.logEvent("SCREEN_${current.name}") {
            param(KEY_PREV_SCREEN, previous?.name.toString())
            if (current is WithPayload) {
                current.payload.forEach { (key, value) ->
                    param(key, value)
                }
            }
        }
    }

    private companion object {
        const val KEY_PREV_SCREEN = "prev_screen"
    }
}
