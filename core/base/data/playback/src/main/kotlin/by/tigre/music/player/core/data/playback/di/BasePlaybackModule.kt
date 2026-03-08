package by.tigre.music.player.core.data.playback.di

import android.content.Context
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.data.playback.impl.PlaybackPlayerImpl
import by.tigre.music.player.tools.coroutines.CoroutineModule

interface BasePlaybackModule {

    val playbackPlayer: PlaybackPlayer

    class Impl(
        context: Context,
        coroutineModule: CoroutineModule
    ) : BasePlaybackModule {
        override val playbackPlayer: PlaybackPlayer by lazy {
            PlaybackPlayerImpl(
                context = context,
                scope = coroutineModule.scope
            )
        }
    }
}
