package by.tigre.music.player.core.data.playback.di

import by.tigre.music.player.core.data.catalog.di.CatalogModule
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.data.playback.impl.PlaybackControllerImpl
import by.tigre.music.player.core.data.storage.playback_queue.di.PlaybackQueueModule
import by.tigre.music.player.tools.coroutines.CoroutineModule

interface PlaybackModule {

    val playbackController: PlaybackController
    val playbackPlayer: PlaybackPlayer

    class Impl(
        coroutineModule: CoroutineModule,
        playbackQueueModule: PlaybackQueueModule,
        catalogModule: CatalogModule,
        basePlaybackModule: BasePlaybackModule
    ) : PlaybackModule {
        override val playbackController: PlaybackController by lazy {
            PlaybackControllerImpl(
                scope = coroutineModule.scope,
                storage = playbackQueueModule.playbackQueueStorage,
                player = playbackPlayer,
                catalog = catalogModule.catalogSource
            )
        }

        override val playbackPlayer: PlaybackPlayer = basePlaybackModule.playbackPlayer
    }
}
