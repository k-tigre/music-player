package by.tigre.music.player.core.data.storage.favorites.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import by.tigre.music.player.core.data.storage.favorites.FavoritesStorage
import by.tigre.music.player.core.data.storage.music.DatabaseMusic
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesStorageImpl(
    private val database: DatabaseMusic,
    scope: CoroutineScope,
) : FavoritesStorage {
    private val coroutineContext = scope.coroutineContext

    override val allSongRows: Flow<List<FavoritesStorage.FavoriteSongRow>> =
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

    override val allArtistRows: Flow<List<FavoritesStorage.FavoriteArtistRow>> =
        database.favoriteArtistQueries.selectAllOrdered(
            mapper = { artistId, addedAt ->
                FavoritesStorage.FavoriteArtistRow(
                    artistId = Artist.Id(artistId),
                    addedAt = addedAt,
                )
            }
        ).asFlow().mapToList(coroutineContext)

    override val allArtistIds: Flow<List<Artist.Id>> =
        database.favoriteArtistQueries.selectAllArtistIds()
            .asFlow()
            .mapToList(coroutineContext)
            .map { rows -> rows.map { Artist.Id(it) } }

    override val allAlbumRows: Flow<List<FavoritesStorage.FavoriteAlbumRow>> =
        database.favoriteAlbumQueries.selectAllOrdered(
            mapper = { artistId, albumId, addedAt ->
                FavoritesStorage.FavoriteAlbumRow(
                    artistId = Artist.Id(artistId),
                    albumId = Album.Id(albumId),
                    addedAt = addedAt,
                )
            }
        ).asFlow().mapToList(coroutineContext)

    override suspend fun addSong(songId: Song.Id, addedAt: Long) {
        database.favoriteSongQueries.insertFavorite(
            song_id = songId.value,
            added_at = addedAt,
        )
    }

    override suspend fun removeSong(songId: Song.Id) {
        database.favoriteSongQueries.deleteFavorite(song_id = songId.value)
    }

    override suspend fun isSongFavorite(songId: Song.Id): Boolean =
        database.favoriteSongQueries.selectIsFavorite(song_id = songId.value)
            .executeAsOne()

    override suspend fun addArtist(artistId: Artist.Id, addedAt: Long) {
        database.favoriteArtistQueries.insertFavorite(
            artist_id = artistId.value,
            added_at = addedAt,
        )
    }

    override suspend fun removeArtist(artistId: Artist.Id) {
        database.favoriteArtistQueries.deleteFavorite(artist_id = artistId.value)
    }

    override suspend fun isArtistFavorite(artistId: Artist.Id): Boolean =
        database.favoriteArtistQueries.selectIsFavorite(artist_id = artistId.value)
            .executeAsOne()

    override suspend fun addAlbum(artistId: Artist.Id, albumId: Album.Id, addedAt: Long) {
        database.favoriteAlbumQueries.insertFavorite(
            artist_id = artistId.value,
            album_id = albumId.value,
            added_at = addedAt,
        )
    }

    override suspend fun removeAlbum(artistId: Artist.Id, albumId: Album.Id) {
        database.favoriteAlbumQueries.deleteFavorite(
            artist_id = artistId.value,
            album_id = albumId.value,
        )
    }

    override suspend fun isAlbumFavorite(artistId: Artist.Id, albumId: Album.Id): Boolean =
        database.favoriteAlbumQueries.selectIsFavorite(
            artist_id = artistId.value,
            album_id = albumId.value,
        ).executeAsOne()
}
