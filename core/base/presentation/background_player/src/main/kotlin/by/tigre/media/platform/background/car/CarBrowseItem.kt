package by.tigre.media.platform.background.car

import android.net.Uri

data class CarBrowseItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val isBrowsable: Boolean,
    val isPlayable: Boolean,
    val artworkUri: Uri? = null,
    val customBrowseActionIds: List<String> = emptyList(),
)
