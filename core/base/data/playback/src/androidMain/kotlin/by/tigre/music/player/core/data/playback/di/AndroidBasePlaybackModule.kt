package by.tigre.music.player.core.data.playback.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import by.tigre.music.player.core.data.playback.AndroidPlaybackPlayer
import by.tigre.music.player.core.data.playback.AppPlaybackVolume
import by.tigre.music.player.core.data.playback.PlaybackEqualizer
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.data.playback.impl.AndroidAppPlaybackVolume
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

    private val appPlaybackVolumeImpl: AndroidAppPlaybackVolume by lazy {
        AndroidAppPlaybackVolume(playerProvider = { impl.player as ExoPlayer })
    }

    override val playbackPlayer: PlaybackPlayer get() = impl

    override val playbackEqualizer: PlaybackEqualizer get() = equalizer

    override val appPlaybackVolume: AppPlaybackVolume get() = appPlaybackVolumeImpl

    val androidPlaybackPlayer: AndroidPlaybackPlayer get() = impl
}
