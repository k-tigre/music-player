package by.tigre.music.player.core.data.catalog.di

import android.content.Context
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.catalog.android.DbHelper
import by.tigre.music.player.core.data.catalog.impl.CatalogSourceImpl

interface CatalogModule {

    val catalogSource: CatalogSource

    class Impl(
        context: Context,
    ) : CatalogModule {
        override val catalogSource: CatalogSource by lazy {
            CatalogSourceImpl(DbHelper.Impl(context))
        }
    }
}
