package by.tigre.music.player.core.data.storage.playlist.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import by.tigre.music.player.core.data.storage.music.DatabaseMusic
import by.tigre.music.player.core.data.storage.playlist.PlaylistStorage
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.entiry.playlist.Playlist
import by.tigre.music.player.core.entiry.playlist.PlaylistKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class PlaylistStorageImpl(
    private val database: DatabaseMusic,
    scope: CoroutineScope,
) : PlaylistStorage {
    private val coroutineContext = scope.coroutineContext

    override val allPlaylists: Flow<List<Playlist>> = database.playlistQueries.selectAllPlaylists(
        mapper = { id, name, kind, _, _, trackCount ->
            Playlist(
                id = Playlist.Id(id),
                name = name,
                kind = kind,
                trackCount = trackCount.toInt(),
            )
        }
    ).asFlow().mapToList(coroutineContext)

    override fun playlistTracks(playlistId: Playlist.Id): Flow<List<PlaylistStorage.PlaylistSongRow>> =
        database.playlistQueries.selectSongsByPlaylistId(
            playlist_id = playlistId.value,
            mapper = { id, songId, sortOrder ->
                PlaylistStorage.PlaylistSongRow(
                    entryId = id,
                    songId = Song.Id(songId),
                    sortOrder = sortOrder.toInt(),
                )
            }
        ).asFlow().mapToList(coroutineContext)

    override suspend fun createPlaylist(name: String): Playlist.Id {
        val now = System.currentTimeMillis()
        database.playlistQueries.insertPlaylist(
            name = name,
            kind = PlaylistKind.User,
            created_at = now,
            updated_at = now,
        )

        return Playlist.Id(database.playlistQueries.lastInsertRowId().executeAsOne())
    }

    override suspend fun renamePlaylist(id: Playlist.Id, name: String) {
        database.playlistQueries.updatePlaylistName(
            name = name,
            updated_at = System.currentTimeMillis(),
            id = id.value,
        )
    }

    override suspend fun deletePlaylist(id: Playlist.Id) {
        database.playlistQueries.deletePlaylist(id = id.value)
    }

    override suspend fun addSongs(playlistId: Playlist.Id, songIds: List<Song.Id>) {
        if (songIds.isEmpty()) return
        database.playlistQueries.transaction {
            var sortOrder = database.playlistQueries.selectMaxSortOrder(playlist_id = playlistId.value)
                .executeAsOne()
                .toInt()
            songIds.forEach { songId ->
                sortOrder += 1
                database.playlistQueries.insertPlaylistSong(
                    playlist_id = playlistId.value,
                    song_id = songId.value,
                    sort_order = sortOrder.toLong(),
                )
            }
        }
    }

    override suspend fun removeSong(entryId: Long) {
        database.playlistQueries.deletePlaylistSongByEntryId(id = entryId)
    }

    override suspend fun updateSortOrders(updates: List<Pair<Long, Int>>) {
        if (updates.isEmpty()) return
        database.playlistQueries.transaction {
            updates.forEach { (entryId, sortOrder) ->
                database.playlistQueries.updateSortOrder(
                    sort_order = sortOrder.toLong(),
                    id = entryId,
                )
            }
        }
    }
}
