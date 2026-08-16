package by.tigre.media.platform.player.eq

import by.tigre.media.platform.playback.eq.EqProfileController
import by.tigre.media.platform.tools.analytics.common.CommonEventAnalytics
import by.tigre.media.platform.tools.analytics.common.CommonEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Bridges EQ profile lifecycle → Mixpanel/common analytics for all apps. */
fun CoroutineScope.bindEqProfileAnalytics(
    controller: EqProfileController,
    analytics: CommonEventAnalytics,
) {
    launch {
        controller.lifecycleEvents.collect { event ->
            analytics.trackEvent(
                CommonEvents.Action.EqProfileResolved(
                    outcome = event.outcome,
                    matchLevel = event.matchLevel.name.lowercase(),
                    source = event.source?.storageName ?: "none",
                    routeKind = event.routeKind,
                    contentKind = event.contentKind,
                ),
            )
        }
    }
}
