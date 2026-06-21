package by.tigre.music.player.core.entiry.playlist

data class Playlist(
    val id: Id,
    val name: String,
    val kind: PlaylistKind,
    val trackCount: Int,
) {
    @JvmInline
    value class Id(val value: Long)
}
