package by.tigre.music.player.tools.analytics.music

import by.tigre.music.player.tools.analytics.common.CommonEvents
import by.tigre.music.player.tools.analytics.common.Tracker

internal class MusicEventAnalyticsImpl(
    private val tracker: Tracker,
) : MusicEventAnalytics {
    override fun trackEvent(event: CommonEvents.Action) = tracker.trackAction(event)
    override fun trackEvent(event: MusicEvents.Action) = tracker.trackAction(event)
}
