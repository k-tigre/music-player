package by.tigre.music.player.core.presentation.catalog.entiry

import com.arkivanov.essenty.parcelable.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Artist(
    val id: Long,
    val name: String,
    val albums: Int,
    val songs: Int
) : Parcelable
