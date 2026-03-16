package by.tigre.music.player.core.data.playback

import androidx.media3.common.Player

interface AndroidPlaybackPlayer : PlaybackPlayer {
    val player: Player
}
