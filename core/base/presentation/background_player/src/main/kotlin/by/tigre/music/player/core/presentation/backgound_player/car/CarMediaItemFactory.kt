package by.tigre.music.player.core.presentation.backgound_player.car

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

internal object CarMediaItemFactory {

    fun rootItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(CarMediaIds.ROOT)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle("")
                    .build()
            )
            .build()

    fun fromBrowseItem(item: CarBrowseItem, mediaType: Int): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(item.title)
            .setMediaType(mediaType)
            .setIsBrowsable(item.isBrowsable)
            .setIsPlayable(item.isPlayable)
        item.subtitle?.let { metadata.setArtist(it) }
        item.artworkUri?.let { metadata.setArtworkUri(it) }
        return MediaItem.Builder()
            .setMediaId(item.id)
            .setMediaMetadata(metadata.build())
            .build()
    }
}
