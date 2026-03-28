package by.tigre.music.player.core.presentation.catalog.component

data class PlayerItem(
    val title: String,
    val subtitle: String,
    val artist: String? = null,
    val album: String? = null,
    val coverUri: Any? = null
)
