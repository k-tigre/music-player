package by.tigre.media.platform.tools.analytics.music

import by.tigre.media.platform.tools.analytics.common.CommonEvents
import by.tigre.media.platform.tools.analytics.common.Tracker

internal class MusicEventAnalyticsImpl(
    private val tracker: Tracker,
) : MusicEventAnalytics {
    override fun trackEvent(event: CommonEvents.Action) = tracker.trackAction(event)
    override fun trackEvent(event: MusicEvents.Action) = tracker.trackAction(event)
}
