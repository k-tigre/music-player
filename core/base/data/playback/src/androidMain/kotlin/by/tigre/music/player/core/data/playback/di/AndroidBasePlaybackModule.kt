package by.tigre.music.player.core.data.playback.di

import android.content.Context
import by.tigre.music.player.core.data.playback.AndroidPlaybackPlayer
import by.tigre.music.player.core.data.playback.PlaybackEqualizer
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.data.playback.impl.AndroidPlaybackEqualizer
import by.tigre.music.player.core.data.playback.impl.PlaybackPlayerImpl
import by.tigre.music.player.core.data.playback.prefs.EqualizerPreferences
import by.tigre.music.player.core.data.storage.preferences.Preferences
import by.tigre.music.player.tools.coroutines.CoroutineModule

class AndroidBasePlaybackModule(
    context: Context,
    coroutineModule: CoroutineModule,
    preferences: Preferences,
) : BasePlaybackModule {

    private val equalizerPreferences = EqualizerPreferences(preferences)
    private val impl: PlaybackPlayerImpl by lazy {
        PlaybackPlayerImpl(
            context = context,
            scope = coroutineModule.scope
        )
    }

    private val equalizer: AndroidPlaybackEqualizer by lazy {
        AndroidPlaybackEqualizer(impl, equalizerPreferences)
    }

    override val playbackPlayer: PlaybackPlayer get() = impl

    override val playbackEqualizer: PlaybackEqualizer get() = equalizer

    val androidPlaybackPlayer: AndroidPlaybackPlayer get() = impl
}
