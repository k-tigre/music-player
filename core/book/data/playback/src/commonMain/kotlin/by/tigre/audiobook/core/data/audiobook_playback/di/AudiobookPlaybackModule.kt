package by.tigre.audiobook.core.data.audiobook_playback.di

import by.tigre.audiobook.core.data.audiobook.di.AudiobookCatalogModule
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.data.audiobook_playback.impl.AudiobookPlaybackControllerImpl
import by.tigre.audiobook.core.data.audiobook_playback.prefs.AudiobookPlaybackSpeedPreferences
import by.tigre.audiobook.core.data.storage.audiobook_catalog.di.AudiobookCatalogStorageModule
import by.tigre.media.platform.playback.di.BasePlaybackModule
import by.tigre.media.platform.preferences.Preferences
import by.tigre.media.platform.tools.coroutines.CoroutineModule

interface AudiobookPlaybackModule {

    val audiobookPlaybackController: AudiobookPlaybackController

    class Impl(
        audiobookCatalogStorageModule: AudiobookCatalogStorageModule,
        audiobookCatalogModule: AudiobookCatalogModule,
        basePlaybackModule: BasePlaybackModule,
        preferences: Preferences,
        coroutineModule: CoroutineModule
    ) : AudiobookPlaybackModule {

        override val audiobookPlaybackController: AudiobookPlaybackController by lazy {
            AudiobookPlaybackControllerImpl(
                player = basePlaybackModule.playbackPlayer,
                catalog = audiobookCatalogModule.audiobookCatalogSource,
                storage = audiobookCatalogStorageModule.audiobookPlaybackStorage,
                speedPreferences = AudiobookPlaybackSpeedPreferences(preferences),
                scope = coroutineModule.scope
            )
        }
    }
}
