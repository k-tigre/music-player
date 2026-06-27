package by.tigre.music.player.core.data.storage.favorites

import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.flow.Flow

interface FavoritesStorage {
    val allOrdered: Flow<List<FavoriteSongRow>>
    val allSongIds: Flow<List<Song.Id>>

    suspend fun addFavorite(songId: Song.Id, addedAt: Long = System.currentTimeMillis())
    suspend fun removeFavorite(songId: Song.Id)
    suspend fun addFavorites(songIds: List<Song.Id>, addedAt: Long = System.currentTimeMillis())
    suspend fun removeFavorites(songIds: List<Song.Id>)
    suspend fun isFavorite(songId: Song.Id): Boolean

    data class FavoriteSongRow(
        val songId: Song.Id,
        val addedAt: Long,
    )
}
