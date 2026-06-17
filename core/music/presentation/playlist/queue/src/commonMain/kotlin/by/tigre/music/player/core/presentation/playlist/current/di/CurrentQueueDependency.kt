package by.tigre.music.player.core.presentation.playlist.current.di

import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.media.platform.tools.analytics.music.MusicAnalyticsDependency

interface CurrentQueueDependency : MusicAnalyticsDependency {
    val playbackController: PlaybackController
}
