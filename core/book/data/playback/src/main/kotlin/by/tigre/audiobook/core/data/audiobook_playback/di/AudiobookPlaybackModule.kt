package by.tigre.audiobook.core.data.audiobook_playback.di

import by.tigre.audiobook.core.data.audiobook.di.AudiobookCatalogModule
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.data.audiobook_playback.impl.AudiobookPlaybackControllerImpl
import by.tigre.audiobook.core.data.storage.audiobook_catalog.di.AudiobookCatalogStorageModule
import by.tigre.music.player.core.data.playback.di.BasePlaybackModule
import by.tigre.music.player.tools.coroutines.CoroutineModule

interface AudiobookPlaybackModule {

    val audiobookPlaybackController: AudiobookPlaybackController

    class Impl(
        audiobookCatalogStorageModule: AudiobookCatalogStorageModule,
        audiobookCatalogModule: AudiobookCatalogModule,
        basePlaybackModule: BasePlaybackModule,
        coroutineModule: CoroutineModule
    ) : AudiobookPlaybackModule {

        override val audiobookPlaybackController: AudiobookPlaybackController by lazy {
            AudiobookPlaybackControllerImpl(
                player = basePlaybackModule.playbackPlayer,
                catalog = audiobookCatalogModule.audiobookCatalogSource,
                storage = audiobookCatalogStorageModule.audiobookPlaybackStorage,
                scope = coroutineModule.scope
            )
        }
    }
}
