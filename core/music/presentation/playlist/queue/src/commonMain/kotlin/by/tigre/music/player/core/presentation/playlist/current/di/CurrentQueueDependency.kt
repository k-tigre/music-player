package by.tigre.music.player.core.presentation.playlist.current.di

import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.data.playlist.AddToPlaylistCoordinator
import by.tigre.music.player.core.data.playlist.PlaylistRepository
import by.tigre.media.platform.tools.analytics.music.MusicAnalyticsDependency

interface CurrentQueueDependency : MusicAnalyticsDependency {
    val playbackController: PlaybackController
    val playlistRepository: PlaylistRepository
    val addToPlaylistCoordinator: AddToPlaylistCoordinator
    val albumArtProvider: AlbumArtProvider
}
