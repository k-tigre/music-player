package by.tigre.audiobook.core.data.audiobook.di

import android.content.Context
import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook.impl.AudiobookCatalogSourceImpl
import by.tigre.audiobook.core.data.storage.audiobook_catalog.di.AudiobookCatalogStorageModule

interface AudiobookCatalogModule {
    val audiobookCatalogSource: AudiobookCatalogSource

    class Impl(
        context: Context,
        audiobookCatalogStorageModule: AudiobookCatalogStorageModule
    ) : AudiobookCatalogModule {
        override val audiobookCatalogSource: AudiobookCatalogSource by lazy {
            AudiobookCatalogSourceImpl(context, audiobookCatalogStorageModule.audiobookCatalogStorage)
        }
    }
}
