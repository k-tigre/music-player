package by.tigre.music.player.core.presentation.catalog.di

import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.music.player.core.presentation.catalog.component.BasePlaybackController
import by.tigre.media.platform.tools.analytics.common.CommonAnalyticsDependency

interface PlayerDependency : CommonAnalyticsDependency {
    val basePlaybackController: BasePlaybackController
    val playbackEqualizer: PlaybackEqualizer
    val appPlaybackVolume: AppPlaybackVolume?
}
