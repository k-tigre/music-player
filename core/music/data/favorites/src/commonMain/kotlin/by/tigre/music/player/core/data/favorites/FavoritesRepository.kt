package by.tigre.music.player.core.data.favorites

import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    val favoriteIds: Flow<Set<Song.Id>>
    val tracks: Flow<List<FavoriteTrack>>
    val likedAlbums: Flow<List<LikedAlbum>>
    val likedArtists: Flow<List<LikedArtist>>

    suspend fun toggle(songId: Song.Id): Boolean
    suspend fun setFavorite(songId: Song.Id, favorite: Boolean)
    suspend fun toggleAlbum(artistId: Artist.Id, albumId: Album.Id): Boolean
    suspend fun toggleArtist(artistId: Artist.Id): Boolean
    suspend fun resolvePlayableSongIds(): List<Song.Id>

    data class FavoriteTrack(
        val songId: Song.Id,
        val addedAt: Long,
        val song: Song?,
    )

    data class LikedAlbum(
        val artistId: Artist.Id,
        val album: Album,
        val likedSongCount: Int,
        val lastLikedAt: Long,
    )

    data class LikedArtist(
        val artist: Artist,
        val likedSongCount: Int,
        val lastLikedAt: Long,
    )
}
