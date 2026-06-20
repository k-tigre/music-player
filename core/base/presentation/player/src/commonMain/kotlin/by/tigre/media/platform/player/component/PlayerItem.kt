package by.tigre.media.platform.player.component

data class PlayerItem(
    val title: String,
    val subtitle: String,
    val artist: String? = null,
    val album: String? = null,
    val coverUri: Any? = null,
    val isExternal: Boolean = false,
    val canReturnToQueue: Boolean = false,
)
