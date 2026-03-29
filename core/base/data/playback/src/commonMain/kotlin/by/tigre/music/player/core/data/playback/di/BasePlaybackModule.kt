package by.tigre.music.player.core.data.playback.di

import by.tigre.music.player.core.data.playback.AppPlaybackVolume
import by.tigre.music.player.core.data.playback.PlaybackEqualizer
import by.tigre.music.player.core.data.playback.PlaybackPlayer

interface BasePlaybackModule {
    val playbackPlayer: PlaybackPlayer
    val playbackEqualizer: PlaybackEqualizer
    val appPlaybackVolume: AppPlaybackVolume?
        get() = null
}
