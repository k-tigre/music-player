package by.tigre.music.player.core.data.playback.di

import android.content.Context
import by.tigre.music.player.core.data.playback.AndroidPlaybackPlayer
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.data.playback.impl.PlaybackPlayerImpl
import by.tigre.music.player.tools.coroutines.CoroutineModule

class AndroidBasePlaybackModule(
    context: Context,
    coroutineModule: CoroutineModule
) : BasePlaybackModule {
    private val impl: PlaybackPlayerImpl by lazy {
        PlaybackPlayerImpl(
            context = context,
            scope = coroutineModule.scope
        )
    }

    override val playbackPlayer: PlaybackPlayer get() = impl

    val androidPlaybackPlayer: AndroidPlaybackPlayer get() = impl
}
