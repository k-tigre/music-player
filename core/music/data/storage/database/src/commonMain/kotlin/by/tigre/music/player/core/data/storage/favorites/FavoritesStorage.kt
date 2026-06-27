package by.tigre.music.player.core.data.storage.favorites

import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.flow.Flow

interface FavoritesStorage {
    val allSongRows: Flow<List<FavoriteSongRow>>
    val allSongIds: Flow<List<Song.Id>>
    val allArtistRows: Flow<List<FavoriteArtistRow>>
    val allArtistIds: Flow<List<Artist.Id>>
    val allAlbumRows: Flow<List<FavoriteAlbumRow>>

    suspend fun addSong(songId: Song.Id, addedAt: Long = System.currentTimeMillis())
    suspend fun removeSong(songId: Song.Id)
    suspend fun isSongFavorite(songId: Song.Id): Boolean

    suspend fun addArtist(artistId: Artist.Id, addedAt: Long = System.currentTimeMillis())
    suspend fun removeArtist(artistId: Artist.Id)
    suspend fun isArtistFavorite(artistId: Artist.Id): Boolean

    suspend fun addAlbum(
        artistId: Artist.Id,
        albumId: Album.Id,
        addedAt: Long = System.currentTimeMillis(),
    )
    suspend fun removeAlbum(artistId: Artist.Id, albumId: Album.Id)
    suspend fun isAlbumFavorite(artistId: Artist.Id, albumId: Album.Id): Boolean

    data class FavoriteSongRow(
        val songId: Song.Id,
        val addedAt: Long,
    )

    data class FavoriteArtistRow(
        val artistId: Artist.Id,
        val addedAt: Long,
    )

    data class FavoriteAlbumRow(
        val artistId: Artist.Id,
        val albumId: Album.Id,
        val addedAt: Long,
    )
}
