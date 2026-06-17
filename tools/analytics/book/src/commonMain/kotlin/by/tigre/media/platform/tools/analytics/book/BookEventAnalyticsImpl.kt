package by.tigre.media.platform.tools.analytics.book

import by.tigre.media.platform.tools.analytics.common.CommonEvents
import by.tigre.media.platform.tools.analytics.common.Tracker

internal class BookEventAnalyticsImpl(
    private val tracker: Tracker,
) : BookEventAnalytics {
    override fun trackEvent(event: CommonEvents.Action) = tracker.trackAction(event)
    override fun trackEvent(event: AudiobookEvents.Action) = tracker.trackAction(event)
}
