package by.tigre.music.player.presentation.root.di

import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.platform.PlayerSettings
import by.tigre.music.player.platform.ThemeSettingsStore
import by.tigre.media.platform.tools.analytics.music.MusicAnalyticsDependency

interface RootDependency : MusicAnalyticsDependency {
    val playbackQueueStorage: PlaybackQueueStorage
    val playerSettings: PlayerSettings
    val themeSettingsStore: ThemeSettingsStore
}
