package by.tigre.music.player.core.entiry.playback

data class Queue(
    val items: List<Long>,
    val playingItemIndex: Int
)
