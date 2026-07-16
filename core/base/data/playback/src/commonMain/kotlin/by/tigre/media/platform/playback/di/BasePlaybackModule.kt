package by.tigre.media.platform.playback.di

import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.AudioSpectrumSource
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.PlaybackPlayer
import by.tigre.media.platform.playback.prefs.VisualizerPreferences

interface BasePlaybackModule {
    val playbackPlayer: PlaybackPlayer
    val playbackEqualizer: PlaybackEqualizer
    val audioSpectrumSource: AudioSpectrumSource
    val visualizerPreferences: VisualizerPreferences
    val appPlaybackVolume: AppPlaybackVolume?
        get() = null
}
