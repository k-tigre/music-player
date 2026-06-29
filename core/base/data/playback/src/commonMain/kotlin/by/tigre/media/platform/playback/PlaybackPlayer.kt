package by.tigre.media.platform.playback

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PlaybackPlayer {
    val progress: Flow<Progress>
    val state: StateFlow<State>
    val playbackSpeed: StateFlow<Float>

    suspend fun stop()
    suspend fun pause()
    suspend fun resume()
    suspend fun seekTo(position: Long)
    suspend fun setMediaItem(item: MediaItemWrapper, position: Long)
    suspend fun setPlaybackSpeed(speed: Float)

    enum class State { Idle, Paused, Playing, Ended }
    data class Progress(val position: Long, val duration: Long)
}
