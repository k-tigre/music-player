package by.tigre.music.player.core.presentation.backgound_player.car

import android.net.Uri

data class CarBrowseItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val isBrowsable: Boolean,
    val isPlayable: Boolean,
    val artworkUri: Uri? = null,
)
