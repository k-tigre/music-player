package by.tigre.music.player.core.data.playlist.impl

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playlist.PlaylistRepository
import by.tigre.music.player.core.data.storage.playlist.PlaylistStorage
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.entiry.playlist.Playlist
import by.tigre.music.player.core.entiry.playlist.PlaylistTrackEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PlaylistRepositoryImpl(
    private val playlistStorage: PlaylistStorage,
    private val catalogSource: CatalogSource,
) : PlaylistRepository {

    override val allPlaylists: Flow<List<Playlist>> = playlistStorage.allPlaylists

    override fun tracks(playlistId: Playlist.Id): Flow<List<PlaylistTrackEntry>> =
        combine(
            playlistStorage.playlistTracks(playlistId),
            catalogSource.dataVersion,
        ) { rows, _ ->
            rows
        }.map { rows ->
            if (rows.isEmpty()) return@map emptyList()

            val songsById = catalogSource.resolveSongsByIds(
                rows.map { it.songId }.distinct()
            ).associateBy { it.id }

            rows.map { row ->
                PlaylistTrackEntry(
                    entryId = row.entryId,
                    songId = row.songId,
                    song = songsById[row.songId],
                )
            }
        }

    override suspend fun createPlaylist(name: String): Playlist.Id =
        playlistStorage.createPlaylist(name = name)

    override suspend fun renamePlaylist(id: Playlist.Id, name: String) {
        playlistStorage.renamePlaylist(id = id, name = name)
    }

    override suspend fun deletePlaylist(id: Playlist.Id) {
        playlistStorage.deletePlaylist(id = id)
    }

    override suspend fun isNameTaken(name: String, excludeId: Playlist.Id?): Boolean {
        val normalized = name.trim()
        if (normalized.isEmpty()) return false
        return allPlaylists.first().any { playlist ->
            playlist.id != excludeId && playlist.name.equals(normalized, ignoreCase = true)
        }
    }

    override suspend fun addSongs(playlistId: Playlist.Id, songIds: List<Song.Id>) {
        playlistStorage.addSongs(playlistId = playlistId, songIds = songIds)
    }

    override suspend fun removeTrack(entryId: Long) {
        playlistStorage.removeSong(entryId = entryId)
    }

    override suspend fun reorderTracks(updates: List<Pair<Long, Int>>) {
        playlistStorage.updateSortOrders(updates = updates)
    }

    override suspend fun resolvePlayableSongIds(playlistId: Playlist.Id): List<Song.Id> {
        val orderedSongIds = playlistStorage.playlistTracks(playlistId)
            .first()
            .map { it.songId }

        if (orderedSongIds.isEmpty()) return emptyList()

        val resolvedSongIds = catalogSource.resolveSongsByIds(
            orderedSongIds.distinct()
        ).associateBy { it.id }

        return orderedSongIds.mapNotNull { songId ->
            resolvedSongIds[songId]?.id
        }
    }
}
