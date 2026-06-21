package by.tigre.music.player.core.entiry.playlist

import by.tigre.music.player.core.entiry.catalog.Song

data class PlaylistTrackEntry(
    val entryId: Long,
    val songId: Song.Id,
    val song: Song?,
)
