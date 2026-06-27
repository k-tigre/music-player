package by.tigre.music.player.core.entiry.playback

import by.tigre.music.player.core.entiry.playlist.Playlist

sealed interface QueueSession {
    data object Plain : QueueSession

    data class FromPlaylist(
        val playlistId: Playlist.Id,
        val name: String,
        val isDirty: Boolean,
    ) : QueueSession
}
