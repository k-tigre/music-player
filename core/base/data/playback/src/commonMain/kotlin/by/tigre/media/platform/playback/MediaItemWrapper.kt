package by.tigre.media.platform.playback

data class MediaItemWrapper(
    val uri: String,
    val title: String,
    val artist: String? = null,
    val albumTitle: String? = null
)
