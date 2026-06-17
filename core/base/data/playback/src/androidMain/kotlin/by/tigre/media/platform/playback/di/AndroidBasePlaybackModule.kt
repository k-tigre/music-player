package by.tigre.media.platform.playback.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import by.tigre.media.platform.playback.AndroidPlaybackPlayer
import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.PlaybackPlayer
import by.tigre.media.platform.playback.impl.AndroidAppPlaybackVolume
import by.tigre.media.platform.playback.impl.AndroidPlaybackEqualizer
import by.tigre.media.platform.playback.impl.PlaybackPlayerImpl
import by.tigre.media.platform.playback.prefs.EqualizerPreferences
import by.tigre.media.platform.preferences.Preferences
import by.tigre.media.platform.tools.coroutines.CoroutineModule

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
