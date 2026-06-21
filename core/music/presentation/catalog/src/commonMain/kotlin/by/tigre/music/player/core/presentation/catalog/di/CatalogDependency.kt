package by.tigre.music.player.core.presentation.catalog.di

import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playlist.AddToPlaylistCoordinator
import by.tigre.music.player.core.data.playlist.PlaylistRepository
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.media.platform.tools.analytics.music.MusicAnalyticsDependency

interface CatalogDependency : MusicAnalyticsDependency {
    val catalogSource: CatalogSource
    val playbackController: PlaybackController
    val addToPlaylistCoordinator: AddToPlaylistCoordinator
    val playlistRepository: PlaylistRepository
    val albumArtProvider: AlbumArtProvider
}
