package by.tigre.audiobook.core.presentation.audiobook_catalog.di

import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.music.player.tools.analytics.book.BookAnalyticsDependency

interface AudiobookCatalogDependency : BookAnalyticsDependency {
    val audiobookCatalogSource: AudiobookCatalogSource
    val audiobookPlaybackController: AudiobookPlaybackController
}
