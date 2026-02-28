package by.tigre.music.player.core.entiry.catalog

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Artist(
    val id: Id,
    val name: String,
    val songCount: Int,
    val albumCount: Int
) : Parcelable {

    @JvmInline
    @Parcelize
    @Serializable
    value class Id(val value: Long) : Parcelable
}
