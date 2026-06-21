package by.tigre.music.player.core.data.playlist

import by.tigre.music.player.core.entiry.catalog.Song

data class AddToPlaylistRequest(
    val songIds: List<Song.Id>,
    val previewText: String,
)
