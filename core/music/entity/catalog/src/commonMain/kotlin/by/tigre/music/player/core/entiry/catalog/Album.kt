package by.tigre.music.player.core.entiry.catalog

import kotlinx.serialization.Serializable

@Serializable
data class Album(
    val id: Id,
    val name: String,
    val songCount: Int,
    val years: String
) {

    @JvmInline
    @Serializable
    value class Id(val value: Long)
}
