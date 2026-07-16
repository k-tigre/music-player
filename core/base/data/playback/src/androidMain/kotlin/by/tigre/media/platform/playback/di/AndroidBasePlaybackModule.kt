package by.tigre.media.platform.playback.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import by.tigre.media.platform.playback.AndroidPlaybackPlayer
import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.AudioSpectrumSource
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.PlaybackPlayer
import by.tigre.media.platform.playback.impl.AndroidAppPlaybackVolume
import by.tigre.media.platform.playback.impl.AndroidAudioSpectrumSource
import by.tigre.media.platform.playback.impl.AndroidPlaybackEqualizer
import by.tigre.media.platform.playback.impl.PlaybackPlayerImpl
import by.tigre.media.platform.playback.impl.TrackPcmLevelMeter
import by.tigre.media.platform.playback.prefs.EqualizerPreferences
import by.tigre.media.platform.playback.prefs.VisualizerPreferences
import by.tigre.media.platform.preferences.Preferences
import by.tigre.media.platform.tools.coroutines.CoroutineModule

class AndroidBasePlaybackModule(
    context: Context,
    coroutineModule: CoroutineModule,
    preferences: Preferences,
) : BasePlaybackModule {

    private val trackLevelMeter = TrackPcmLevelMeter()
    private val equalizerPreferences = EqualizerPreferences(preferences)
    private val visualizerPrefs = VisualizerPreferences(preferences)
    private val impl: PlaybackPlayerImpl by lazy {
        PlaybackPlayerImpl(
            context = context,
            scope = coroutineModule.scope,
            trackLevelMeter = trackLevelMeter,
        )
    }

    private val equalizer: AndroidPlaybackEqualizer by lazy {
        AndroidPlaybackEqualizer(impl, equalizerPreferences)
    }

    private val spectrum: AndroidAudioSpectrumSource by lazy {
        AndroidAudioSpectrumSource(context, impl, trackLevelMeter, visualizerPrefs)
    }

    private val appPlaybackVolumeImpl: AndroidAppPlaybackVolume by lazy {
        AndroidAppPlaybackVolume(playerProvider = { impl.player as ExoPlayer })
    }

    override val playbackPlayer: PlaybackPlayer get() = impl

    override val playbackEqualizer: PlaybackEqualizer get() = equalizer

    override val audioSpectrumSource: AudioSpectrumSource get() = spectrum

    override val visualizerPreferences: VisualizerPreferences get() = visualizerPrefs

    override val appPlaybackVolume: AppPlaybackVolume get() = appPlaybackVolumeImpl

    val androidPlaybackPlayer: AndroidPlaybackPlayer get() = impl
}
