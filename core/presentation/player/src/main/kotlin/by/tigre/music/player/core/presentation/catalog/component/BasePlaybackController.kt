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
    fun setOrderMode(isNormal: Boolean)
}
