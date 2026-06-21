package by.tigre.music.player.core.data.playlist.di

import by.tigre.music.player.core.data.catalog.di.CatalogModule
import by.tigre.music.player.core.data.playlist.AddToPlaylistCoordinator
import by.tigre.music.player.core.data.playlist.PlaylistRepository
import by.tigre.music.player.core.data.playlist.impl.PlaylistRepositoryImpl
import by.tigre.music.player.core.data.storage.playback_queue.di.PlaybackQueueModule

interface PlaylistModule {
    val playlistRepository: PlaylistRepository
    val addToPlaylistCoordinator: AddToPlaylistCoordinator

    class Impl(
        playbackQueueModule: PlaybackQueueModule,
        catalogModule: CatalogModule,
    ) : PlaylistModule {
        override val playlistRepository: PlaylistRepository by lazy {
            PlaylistRepositoryImpl(
                playlistStorage = playbackQueueModule.playlistStorage,
                catalogSource = catalogModule.catalogSource,
            )
        }

        override val addToPlaylistCoordinator: AddToPlaylistCoordinator by lazy {
            AddToPlaylistCoordinator()
        }
    }
}
