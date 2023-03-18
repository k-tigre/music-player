package by.tigre.music.player.core.data.playback

import by.tigre.music.player.core.data.entiry.playback.MediaItemWrapper
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PlaybackPlayer {
    val player: Player
    val progress: Flow<Progress>
    val state: StateFlow<State>

    suspend fun stop()
    suspend fun pause()
    suspend fun resume()
    suspend fun seekTo(position: Long)
    suspend fun setMediaItem(item: MediaItemWrapper, position: Long)

    enum class State { Idle, Paused, Playing,  Ended }
    data class Progress(val position: Long, val duration: Long)
}
