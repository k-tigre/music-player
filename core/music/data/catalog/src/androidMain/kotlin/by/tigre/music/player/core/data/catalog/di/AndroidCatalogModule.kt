package by.tigre.music.player.core.data.catalog.di

import android.content.Context
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.catalog.android.AndroidCatalogBackend
import by.tigre.music.player.core.data.catalog.android.DbHelper
import by.tigre.music.player.core.data.catalog.hidden.HiddenCatalogStorage
import by.tigre.music.player.core.data.catalog.hidden.HiddenCatalogStorageImpl
import by.tigre.music.player.core.data.catalog.impl.CatalogSourceImpl
import by.tigre.music.player.core.data.storage.preferences.Preferences

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
}
