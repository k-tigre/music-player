package by.tigre.music.player.core.entiry.catalog

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Album(
    val id: Id,
    val name: String,
    val songCount: Int,
    val years: String
) : Parcelable {

    @JvmInline
    @Parcelize
    value class Id(val value: Long) : Parcelable
}
