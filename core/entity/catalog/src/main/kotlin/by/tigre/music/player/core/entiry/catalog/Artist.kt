package by.tigre.music.player.core.entiry.catalog

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int
) : Parcelable
