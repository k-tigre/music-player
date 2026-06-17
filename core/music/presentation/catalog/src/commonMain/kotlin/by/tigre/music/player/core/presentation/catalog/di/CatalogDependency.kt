package by.tigre.music.player.core.presentation.catalog.di

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.media.platform.tools.analytics.music.MusicAnalyticsDependency

interface CatalogDependency : MusicAnalyticsDependency {
    val catalogSource: CatalogSource
    val playbackController: PlaybackController
}
