package by.tigre.music.player.core.presentation.playlist.library.di

import by.tigre.media.platform.tools.analytics.music.MusicAnalyticsDependency
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.data.playlist.AddToPlaylistCoordinator
import by.tigre.music.player.core.data.playlist.PlaylistRepository

interface PlaylistsDependency : MusicAnalyticsDependency {
    val playlistRepository: PlaylistRepository
    val playbackController: PlaybackController
    val catalogSource: CatalogSource
    val addToPlaylistCoordinator: AddToPlaylistCoordinator
}
