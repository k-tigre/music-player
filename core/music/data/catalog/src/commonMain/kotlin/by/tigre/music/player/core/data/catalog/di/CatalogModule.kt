package by.tigre.music.player.core.data.catalog.di

import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.data.catalog.CatalogSource

interface CatalogModule {
    val catalogSource: CatalogSource
    val albumArtProvider: AlbumArtProvider
}
