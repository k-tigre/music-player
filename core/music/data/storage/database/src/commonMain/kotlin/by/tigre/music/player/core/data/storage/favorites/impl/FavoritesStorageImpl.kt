package by.tigre.music.player.core.data.storage.favorites.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import by.tigre.music.player.core.data.storage.favorites.FavoritesStorage
import by.tigre.music.player.core.data.storage.music.DatabaseMusic
import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesStorageImpl(
    private val database: DatabaseMusic,
    scope: CoroutineScope,
) : FavoritesStorage {
    private val coroutineContext = scope.coroutineContext

    override val allOrdered: Flow<List<FavoritesStorage.FavoriteSongRow>> =
        database.favoriteSongQueries.selectAllOrdered(
            mapper = { songId, addedAt ->
                FavoritesStorage.FavoriteSongRow(
                    songId = Song.Id(songId),
                    addedAt = addedAt,
                )
            }
        ).asFlow().mapToList(coroutineContext)

    override val allSongIds: Flow<List<Song.Id>> =
        database.favoriteSongQueries.selectAllSongIds()
            .asFlow()
            .mapToList(coroutineContext)
            .map { rows -> rows.map { Song.Id(it) } }

    override suspend fun addFavorite(songId: Song.Id, addedAt: Long) {
        database.favoriteSongQueries.insertFavorite(
            song_id = songId.value,
            added_at = addedAt,
        )
    }

    override suspend fun removeFavorite(songId: Song.Id) {
        database.favoriteSongQueries.deleteFavorite(song_id = songId.value)
    }

    override suspend fun addFavorites(songIds: List<Song.Id>, addedAt: Long) {
        if (songIds.isEmpty()) return
        database.favoriteSongQueries.transaction {
            songIds.forEach { songId ->
                database.favoriteSongQueries.insertFavorite(
                    song_id = songId.value,
                    added_at = addedAt,
                )
            }
        }
    }

    override suspend fun removeFavorites(songIds: List<Song.Id>) {
        if (songIds.isEmpty()) return
        database.favoriteSongQueries.deleteFavoritesByIds(
            song_id = songIds.map { it.value },
        )
    }

    override suspend fun isFavorite(songId: Song.Id): Boolean =
        database.favoriteSongQueries.selectIsFavorite(song_id = songId.value)
            .executeAsOne()
}
