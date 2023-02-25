package by.tigre.music.player.core.presentation.catalog.entiry

import com.arkivanov.essenty.parcelable.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Album(
    val id: Long,
    val name: String,
    val songs: Int,
    val years: String?,
) : Parcelable
