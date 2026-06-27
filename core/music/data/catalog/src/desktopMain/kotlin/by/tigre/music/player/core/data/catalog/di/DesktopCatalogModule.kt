package by.tigre.music.player.core.data.catalog.di

import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.catalog.desktop.DesktopAlbumArtProvider
import by.tigre.music.player.core.data.catalog.desktop.DesktopCatalogSourceImpl
import by.tigre.music.player.core.data.catalog.hidden.HiddenCatalogStorageImpl
import by.tigre.music.player.core.data.catalog.impl.CatalogSourceImpl
import by.tigre.media.platform.preferences.Preferences
import java.io.File

class DesktopCatalogModule(
    dbDir: File,
    preferences: Preferences,
) : CatalogModule {

    private val backend: DesktopCatalogSourceImpl by lazy { DesktopCatalogSourceImpl(dbDir) }

    override val catalogSource: CatalogSource by lazy {
        CatalogSourceImpl(
            backend = backend,
            hidden = HiddenCatalogStorageImpl(preferences),
        )
    }

    override val albumArtProvider: AlbumArtProvider by lazy {
        DesktopAlbumArtProvider(File(dbDir, "album_art"), backend::getFirstSongPathForAlbum)
    }

    suspend fun addFolder(folder: File) = backend.addFolder(folder)
}
