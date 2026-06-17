package by.tigre.media.platform.playback.di

import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.PlaybackPlayer

interface BasePlaybackModule {
    val playbackPlayer: PlaybackPlayer
    val playbackEqualizer: PlaybackEqualizer
    val appPlaybackVolume: AppPlaybackVolume?
        get() = null
}
