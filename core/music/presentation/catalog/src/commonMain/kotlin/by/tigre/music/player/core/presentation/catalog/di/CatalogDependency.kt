package by.tigre.music.player.core.presentation.catalog.di

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.tools.analytics.AnalyticsDependency

interface CatalogDependency : AnalyticsDependency {
    val catalogSource: CatalogSource
    val playbackController: PlaybackController
}
