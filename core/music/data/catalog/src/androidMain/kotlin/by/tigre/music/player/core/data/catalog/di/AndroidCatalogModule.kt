package by.tigre.music.player.core.data.catalog.di

import android.content.Context
import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.data.catalog.ArtistArtProvider
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.catalog.android.AndroidAlbumArtProvider
import by.tigre.music.player.core.data.catalog.android.AndroidCatalogBackend
import by.tigre.music.player.core.data.catalog.android.DbHelper
import by.tigre.music.player.core.data.catalog.art.ArtistArtProviders
import by.tigre.music.player.core.data.catalog.hidden.HiddenCatalogStorageImpl
import by.tigre.music.player.core.data.catalog.impl.CatalogSourceImpl
import by.tigre.media.platform.preferences.Preferences
import java.io.File

class AndroidCatalogModule(
    context: Context,
    preferences: Preferences,
) : CatalogModule {
    override val catalogSource: CatalogSource by lazy {
        CatalogSourceImpl(
            backend = AndroidCatalogBackend(DbHelper.Impl(context)),
            hidden = HiddenCatalogStorageImpl(preferences),
        )
    }

    override val albumArtProvider: AlbumArtProvider = AndroidAlbumArtProvider()

    override val artistArtProvider: ArtistArtProvider by lazy {
        ArtistArtProviders.create(
            cacheDirPath = File(context.filesDir, "artist_art").absolutePath,
        )
    }
}
