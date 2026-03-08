package by.tigre.music.player.core.entiry.catalog

import kotlinx.serialization.Serializable


@Serializable
data class Artist(
    val id: Id,
    val name: String,
    val songCount: Int,
    val albumCount: Int
) {

    @JvmInline
    @Serializable
    value class Id(val value: Long)
}
