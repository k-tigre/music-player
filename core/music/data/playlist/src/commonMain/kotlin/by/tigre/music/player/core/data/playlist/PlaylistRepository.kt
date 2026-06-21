package by.tigre.music.player.core.data.playlist

import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.entiry.playlist.Playlist
import by.tigre.music.player.core.entiry.playlist.PlaylistTrackEntry
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    val allPlaylists: Flow<List<Playlist>>

    fun tracks(playlistId: Playlist.Id): Flow<List<PlaylistTrackEntry>>

    suspend fun createPlaylist(name: String): Playlist.Id
    suspend fun renamePlaylist(id: Playlist.Id, name: String)
    suspend fun deletePlaylist(id: Playlist.Id)
    suspend fun addSongs(playlistId: Playlist.Id, songIds: List<Song.Id>)
    suspend fun removeTrack(entryId: Long)
    suspend fun reorderTracks(updates: List<Pair<Long, Int>>)

    suspend fun resolvePlayableSongIds(playlistId: Playlist.Id): List<Song.Id>
}
