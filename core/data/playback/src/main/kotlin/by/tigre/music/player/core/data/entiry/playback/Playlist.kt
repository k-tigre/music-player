package by.tigre.music.player.core.data.entiry.playback

data class Playlist(
    val items: List<SongItem>,
    val title: String,
    val playingItemIndex: Int
)
