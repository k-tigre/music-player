package by.tigre.music.player.core.entiry.playback

import by.tigre.music.player.core.entiry.catalog.Song

sealed interface PlayableItem {
    data class CatalogSong(val id: Song.Id) : PlayableItem

    data class ExternalAudio(
        val uri: String,
        val title: String,
        val sourceLabel: String?,
    ) : PlayableItem
}
