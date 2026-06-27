package by.tigre.music.player.core.data.favorites.di

import by.tigre.music.player.core.data.catalog.di.CatalogModule
import by.tigre.music.player.core.data.favorites.FavoritesRepository
import by.tigre.music.player.core.data.favorites.impl.FavoritesRepositoryImpl
import by.tigre.music.player.core.data.storage.playback_queue.di.PlaybackQueueModule

interface FavoritesModule {
    val favoritesRepository: FavoritesRepository

    class Impl(
        playbackQueueModule: PlaybackQueueModule,
        catalogModule: CatalogModule,
    ) : FavoritesModule {
        override val favoritesRepository: FavoritesRepository by lazy {
            FavoritesRepositoryImpl(
                favoritesStorage = playbackQueueModule.favoritesStorage,
                catalogSource = catalogModule.catalogSource,
            )
        }
    }
}
