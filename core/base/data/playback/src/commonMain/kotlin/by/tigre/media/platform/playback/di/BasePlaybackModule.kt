package by.tigre.media.platform.playback.di

import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.PlaybackPlayer
import by.tigre.media.platform.playback.eq.AudioRouteMonitor
import by.tigre.media.platform.playback.eq.EqContentKeyProvider
import by.tigre.media.platform.playback.eq.EqProfileController
import by.tigre.media.platform.playback.eq.EqProfileRepository

interface BasePlaybackModule {
    val playbackPlayer: PlaybackPlayer
    val playbackEqualizer: PlaybackEqualizer
    val appPlaybackVolume: AppPlaybackVolume?
        get() = null

    val eqProfileRepository: EqProfileRepository
    val audioRouteMonitor: AudioRouteMonitor
    val eqContentKeyProvider: EqContentKeyProvider
    val eqProfileController: EqProfileController
}
