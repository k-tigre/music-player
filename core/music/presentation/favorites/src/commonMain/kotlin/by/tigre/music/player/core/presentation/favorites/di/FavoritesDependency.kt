package by.tigre.music.player.core.presentation.favorites.di

import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.favorites.FavoritesRepository
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.media.platform.tools.analytics.music.MusicAnalyticsDependency

interface FavoritesDependency : MusicAnalyticsDependency {
    val favoritesRepository: FavoritesRepository
    val playbackController: PlaybackController
    val catalogSource: CatalogSource
    val albumArtProvider: AlbumArtProvider
}
