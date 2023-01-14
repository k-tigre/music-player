package by.tigre.music.player.core.data.playback

import by.tigre.music.player.core.data.entiry.playback.MediaItemWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PlaybackPlayer {
    val progress: Flow<Progress>
    val state: StateFlow<State>

    fun stop()
    fun pause()
    fun resume()
    fun seekTo(position: Long)
    fun setMediaItem(item: MediaItemWrapper, position: Long)

    enum class State { Idle, Ready, Playing, Buffering, Ended }
    data class Progress(val position: Long, val duration: Long)
}
