package by.tigre.music.player.presentation.root.di

import by.tigre.music.player.platform.PlayerSettings
import by.tigre.media.platform.tools.analytics.music.MusicAnalyticsDependency

interface RootDependency : MusicAnalyticsDependency {
    val playerSettings: PlayerSettings
}
