package by.tigre.audiobook.core.data.audiobook.di

import android.content.Context
import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook.impl.AudiobookCatalogSourceImpl
import by.tigre.audiobook.core.data.audiobook.spaces.LibrarySpacePreferences
import by.tigre.audiobook.core.data.audiobook.spaces.LibrarySpaceRepository
import by.tigre.audiobook.core.data.audiobook.spaces.LibrarySpaceRepositoryImpl
import by.tigre.audiobook.core.data.storage.audiobook_catalog.di.AudiobookCatalogStorageModule
import by.tigre.media.platform.entitlements.EntitlementsRepository
import by.tigre.media.platform.preferences.Preferences
import by.tigre.media.platform.tools.coroutines.CoroutineModule
import kotlinx.coroutines.launch

class AndroidAudiobookCatalogModule(
    context: Context,
    audiobookCatalogStorageModule: AudiobookCatalogStorageModule,
    preferences: Preferences,
    entitlementsRepository: EntitlementsRepository,
    coroutineModule: CoroutineModule,
) : AudiobookCatalogModule {

    override val librarySpaceRepository: LibrarySpaceRepository by lazy {
        LibrarySpaceRepositoryImpl(
            storage = audiobookCatalogStorageModule.audiobookCatalogStorage,
            preferences = LibrarySpacePreferences(preferences),
            entitlements = entitlementsRepository,
        ).also { repo ->
            coroutineModule.scope.launch { repo.ensureInitialized() }
        }
    }

    override val audiobookCatalogSource: AudiobookCatalogSource by lazy {
        AudiobookCatalogSourceImpl(
            context = context,
            storage = audiobookCatalogStorageModule.audiobookCatalogStorage,
            librarySpaceRepository = librarySpaceRepository,
            scope = coroutineModule.scope,
        )
    }
}
