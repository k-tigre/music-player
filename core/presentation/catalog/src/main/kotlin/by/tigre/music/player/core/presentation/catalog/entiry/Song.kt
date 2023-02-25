package by.tigre.music.player.core.presentation.catalog.entiry

import com.arkivanov.essenty.parcelable.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(val id: Long, val name: String) : Parcelable
