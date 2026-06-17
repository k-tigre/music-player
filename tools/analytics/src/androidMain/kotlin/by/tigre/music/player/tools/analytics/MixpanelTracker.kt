package by.tigre.music.player.tools.analytics

import android.content.Context
import by.tigre.music.player.tools.analytics.common.AnalyticsAction
import by.tigre.music.player.tools.analytics.common.AnalyticsScreen
import by.tigre.music.player.tools.analytics.common.Tracker
import by.tigre.music.player.tools.analytics.common.WithPayload
import by.tigre.media.platform.tools.coroutines.CoreScope
import by.tigre.media.platform.tools.coroutines.extensions.tickerFlow
import com.mixpanel.android.mpmetrics.MixpanelAPI
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class MixpanelTracker(
    context: Context,
    mixpanelToken: String,
    serverUrl: String?,
    scope: CoreScope,
) : Tracker {
    private val mixpanel: MixpanelAPI = MixpanelAPI.getInstance(context, mixpanelToken, true)
        .apply {
            setEnableLogging(true)
            if (serverUrl.isNullOrBlank().not()){
                setServerURL(serverUrl)
            }
        }

    init {
        scope.launch {
            tickerFlow(period = FLUSH_PERIOD, initialDelay = FLUSH_DELAY)
                .collect { mixpanel.flush() }
        }
    }

    override fun trackAction(event: AnalyticsAction) {
        mixpanel.trackMap("ACTION:${event.name}", (event as? WithPayload)?.payload)
    }

    override fun trackScreen(previous: AnalyticsScreen?, current: AnalyticsScreen) {
        mixpanel.trackMap(
            "SCREEN:${current.name}",
            mapOf("prev_screen" to previous?.name).run {
                if (current is WithPayload) this + current.payload else this
            }
        )
    }

    private companion object {
        val FLUSH_PERIOD = 5.minutes
        val FLUSH_DELAY = 5.seconds
    }
}
