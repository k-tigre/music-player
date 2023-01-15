package by.tigre.music.player.core.presentation.catalog.entiry

import com.arkivanov.essenty.parcelable.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Artist(val id: String, val name: String) : Parcelable
