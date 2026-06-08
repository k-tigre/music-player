package by.tigre.music.player.core.presentation.playlist.current.di

import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.tools.analytics.AnalyticsDependency

interface CurrentQueueDependency : AnalyticsDependency {
    val playbackController: PlaybackController
}
