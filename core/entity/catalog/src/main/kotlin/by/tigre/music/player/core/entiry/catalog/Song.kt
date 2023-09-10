package by.tigre.music.player.core.entiry.catalog

data class Song(
    val id: Id,
    val name: String,
    val index: String,
    val artist: String,
    val album: String,
    val path: String
) {

    @JvmInline
    value class Id(val value: Long)
}
