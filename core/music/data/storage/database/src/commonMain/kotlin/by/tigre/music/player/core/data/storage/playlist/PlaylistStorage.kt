package by.tigre.music.player.core.data.storage.playlist

import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.entiry.playlist.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistStorage {
    val allPlaylists: Flow<List<Playlist>>
    fun playlistTracks(playlistId: Playlist.Id): Flow<List<PlaylistSongRow>>

    suspend fun createPlaylist(name: String): Playlist.Id
    suspend fun renamePlaylist(id: Playlist.Id, name: String)
    suspend fun deletePlaylist(id: Playlist.Id)
    suspend fun addSongs(playlistId: Playlist.Id, songIds: List<Song.Id>)
    suspend fun removeSong(entryId: Long)
    suspend fun updateSortOrders(updates: List<Pair<Long, Int>>)
    suspend fun replaceTracks(playlistId: Playlist.Id, songIds: List<Song.Id>)

    data class PlaylistSongRow(
        val entryId: Long,
        val songId: Song.Id,
        val sortOrder: Int,
    )
}
