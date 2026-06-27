package by.tigre.music.player.core.data.favorites.impl

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.favorites.FavoritesRepository
import by.tigre.music.player.core.data.storage.favorites.FavoritesStorage
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class FavoritesRepositoryImpl(
    private val favoritesStorage: FavoritesStorage,
    private val catalogSource: CatalogSource,
) : FavoritesRepository {

    override val favoriteIds: Flow<Set<Song.Id>> =
        favoritesStorage.allSongIds.map { it.toSet() }

    override val favoriteArtistIds: Flow<Set<Artist.Id>> =
        favoritesStorage.allArtistIds.map { it.toSet() }

    override val tracks: Flow<List<FavoritesRepository.FavoriteTrack>> =
        combine(
            favoritesStorage.allSongRows,
            catalogSource.dataVersion,
        ) { rows, _ -> rows }
            .map { rows ->
                if (rows.isEmpty()) return@map emptyList()

                val songsById = catalogSource.resolveSongsByIds(
                    rows.map { it.songId }.distinct()
                ).associateBy { it.id }

                rows.map { row ->
                    FavoritesRepository.FavoriteTrack(
                        songId = row.songId,
                        addedAt = row.addedAt,
                        song = songsById[row.songId],
                    )
                }
            }

    override val likedAlbums: Flow<List<FavoritesRepository.LikedAlbum>> =
        combine(
            favoritesStorage.allAlbumRows,
            catalogSource.dataVersion,
        ) { rows, _ -> rows }
            .map { rows ->
                if (rows.isEmpty()) return@map emptyList()

                rows.mapNotNull { row ->
                    val album = catalogSource.getAlbumById(row.artistId, row.albumId) ?: return@mapNotNull null
                    FavoritesRepository.LikedAlbum(
                        artistId = row.artistId,
                        album = album,
                        addedAt = row.addedAt,
                    )
                }
            }

    override val likedArtists: Flow<List<FavoritesRepository.LikedArtist>> =
        combine(
            favoritesStorage.allArtistRows,
            catalogSource.dataVersion,
        ) { rows, _ -> rows }
            .map { rows ->
                if (rows.isEmpty()) return@map emptyList()

                rows.mapNotNull { row ->
                    val artist = catalogSource.getArtistById(row.artistId) ?: return@mapNotNull null
                    FavoritesRepository.LikedArtist(
                        artist = artist,
                        addedAt = row.addedAt,
                    )
                }
            }

    override suspend fun toggle(songId: Song.Id): Boolean =
        if (favoritesStorage.isSongFavorite(songId)) {
            favoritesStorage.removeSong(songId)
            false
        } else {
            favoritesStorage.addSong(songId)
            true
        }

    override suspend fun setFavorite(songId: Song.Id, favorite: Boolean) {
        if (favorite) {
            favoritesStorage.addSong(songId)
        } else {
            favoritesStorage.removeSong(songId)
        }
    }

    override suspend fun toggleAlbum(artistId: Artist.Id, albumId: Album.Id): Boolean =
        if (favoritesStorage.isAlbumFavorite(artistId, albumId)) {
            favoritesStorage.removeAlbum(artistId, albumId)
            false
        } else {
            favoritesStorage.addAlbum(artistId, albumId)
            true
        }

    override suspend fun toggleArtist(artistId: Artist.Id): Boolean =
        if (favoritesStorage.isArtistFavorite(artistId)) {
            favoritesStorage.removeArtist(artistId)
            false
        } else {
            favoritesStorage.addArtist(artistId)
            true
        }

    override suspend fun isAlbumFavorite(artistId: Artist.Id, albumId: Album.Id): Boolean =
        favoritesStorage.isAlbumFavorite(artistId, albumId)

    override suspend fun isArtistFavorite(artistId: Artist.Id): Boolean =
        favoritesStorage.isArtistFavorite(artistId)

    override suspend fun resolvePlayableSongIds(): List<Song.Id> {
        val orderedSongIds = favoritesStorage.allSongRows.first().map { it.songId }
        if (orderedSongIds.isEmpty()) return emptyList()

        val resolvedSongIds = catalogSource.resolveSongsByIds(orderedSongIds.distinct())
            .associateBy { it.id }

        return orderedSongIds.mapNotNull { songId -> resolvedSongIds[songId]?.id }
    }
}
