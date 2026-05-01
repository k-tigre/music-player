package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.data.playback.PlaybackPlayer
import kotlinx.coroutines.flow.Flow

interface BasePlaybackController {
    val player: PlaybackPlayer
    val currentItem: Flow<PlayerItem?>
    val orderMode: Flow<Boolean>

    fun playNext()
    fun playPrev()
    fun pause()
    fun resume()
    fun stop()
    fun setOrderMode(isNormal: Boolean)

    /** Called when the user releases the seek slider; [positionMs] is the intended time in the current item. */
    fun onSeekPositionCommitted(positionMs: Long) = Unit
}
