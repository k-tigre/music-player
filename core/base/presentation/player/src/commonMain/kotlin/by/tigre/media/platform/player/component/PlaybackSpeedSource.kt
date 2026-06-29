package by.tigre.media.platform.player.component

import kotlinx.coroutines.flow.StateFlow

interface PlaybackSpeedSource {
    val playbackSpeed: StateFlow<Float>
    fun setPlaybackSpeed(speed: Float)
    fun resetPlaybackSpeed()
}
