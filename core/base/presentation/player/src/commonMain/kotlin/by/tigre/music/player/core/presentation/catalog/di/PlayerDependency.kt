package by.tigre.music.player.core.presentation.catalog.di

import by.tigre.music.player.core.data.playback.AppPlaybackVolume
import by.tigre.music.player.core.data.playback.PlaybackEqualizer
import by.tigre.music.player.core.presentation.catalog.component.BasePlaybackController
import by.tigre.music.player.tools.analytics.AnalyticsDependency

interface PlayerDependency : AnalyticsDependency {
    val basePlaybackController: BasePlaybackController
    val playbackEqualizer: PlaybackEqualizer
    val appPlaybackVolume: AppPlaybackVolume?
}
