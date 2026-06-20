package by.tigre.media.platform.player.component

import by.tigre.media.platform.playback.PlaybackPlayer
import kotlinx.coroutines.flow.Flow

interface BasePlaybackController {
    val player: PlaybackPlayer
    val currentItem: Flow<PlayerItem?>
    val shuffleEnabled: Flow<Boolean>
    val repeatMode: Flow<RepeatMode>

    fun playNext()
    fun playPrev()
    fun playNextRemote() = playNext()
    fun playPrevRemote() = playPrev()
    fun pause()
    fun resume()
    fun stop()
    fun toggleShuffle()
    fun cycleRepeat()
    fun resumeInterruptedSession() = Unit

    /** Called when the user releases the seek slider; [positionMs] is the intended time in the current item. */
    fun onSeekPositionCommitted(positionMs: Long) = Unit

    /**
     * Relative seek (e.g. ±15s). Return true if the controller handled cross-item seeking;
     * false to fall back to seeking within the current media item only.
     */
    fun seekBy(deltaMs: Long): Boolean = false
}
