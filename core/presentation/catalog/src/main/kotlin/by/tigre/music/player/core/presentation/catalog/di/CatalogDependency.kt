package by.tigre.music.player.core.presentation.catalog.di

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playback.PlaybackController

interface CatalogDependency {
    val catalogSource: CatalogSource
    val playbackController: PlaybackController
}
