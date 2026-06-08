package by.tigre.music.player.tools.analytics

import android.content.Context
import by.tigre.music.player.tools.coroutines.CoreScope
import by.tigre.music.player.tools.coroutines.extensions.tickerFlow
import com.mixpanel.android.mpmetrics.MixpanelAPI
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class MixpanelTracker(
    context: Context,
    mixpanelToken: String,
    scope: CoreScope,
) : Tracker {
    private val mixpanel = MixpanelAPI.getInstance(context, mixpanelToken, true)

    init {
        scope.launch {
            tickerFlow(period = FLUSH_PERIOD, initialDelay = FLUSH_DELAY)
                .collect { mixpanel.flush() }
        }
    }

    override fun trackEvent(event: Event.Action) {
        mixpanel.trackMap("ACTION:${event.name}", (event as? Event.WithPayload)?.payload)
    }

    override fun trackScreen(previous: Event.Screen?, current: Event.Screen) {
        mixpanel.trackMap(
            "SCREEN:${current.name}",
            mapOf("prev_screen" to previous?.name).run {
                if (current is Event.WithPayload) this + current.payload else this
            }
        )
    }

    private companion object {
        val FLUSH_PERIOD = 5.minutes
        val FLUSH_DELAY = 5.seconds
    }
}
