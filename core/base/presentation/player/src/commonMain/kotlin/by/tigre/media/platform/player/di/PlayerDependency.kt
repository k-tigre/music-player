package by.tigre.media.platform.player.di

import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.player.component.BasePlaybackController
import by.tigre.media.platform.player.component.PlaybackSpeedSource
import by.tigre.media.platform.tools.analytics.common.CommonAnalyticsDependency

interface PlayerDependency : CommonAnalyticsDependency {
    val basePlaybackController: BasePlaybackController
    val playbackEqualizer: PlaybackEqualizer
    val appPlaybackVolume: AppPlaybackVolume?
    val playbackSpeedSource: PlaybackSpeedSource?
        get() = null
}
