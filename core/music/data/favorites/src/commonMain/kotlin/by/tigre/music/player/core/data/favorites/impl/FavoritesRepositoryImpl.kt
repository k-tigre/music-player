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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class FavoritesRepositoryImpl(
    private val favoritesStorage: FavoritesStorage,
    private val catalogSource: CatalogSource,
) : FavoritesRepository {

    override val favoriteIds: Flow<Set<Song.Id>> =
        favoritesStorage.allSongIds.map { it.toSet() }

    override val tracks: Flow<List<FavoritesRepository.FavoriteTrack>> =
        combine(
            favoritesStorage.allOrdered,
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
        combine(tracks, catalogSource.dataVersion) { trackEntries, _ -> trackEntries }
            .flatMapLatest { trackEntries ->
                flow {
                    val playableTracks = trackEntries.mapNotNull { entry ->
                        entry.song?.let { song -> entry to song }
                    }
                    if (playableTracks.isEmpty()) {
                        emit(emptyList())
                        return@flow
                    }

                    val albums = playableTracks
                        .groupBy { (_, song) -> song.artistId to song.albumId }
                        .mapNotNull { (key, grouped) ->
                            val (artistId, albumId) = key
                            val album = catalogSource.getAlbumById(artistId, albumId) ?: return@mapNotNull null
                            FavoritesRepository.LikedAlbum(
                                artistId = artistId,
                                album = album,
                                likedSongCount = grouped.size,
                                lastLikedAt = grouped.maxOf { it.first.addedAt },
                            )
                        }
                        .sortedByDescending { it.lastLikedAt }

                    emit(albums)
                }
            }

    override val likedArtists: Flow<List<FavoritesRepository.LikedArtist>> =
        combine(tracks, catalogSource.dataVersion) { trackEntries, _ -> trackEntries }
            .flatMapLatest { trackEntries ->
                flow {
                    val playableTracks = trackEntries.mapNotNull { entry ->
                        entry.song?.let { song -> entry to song }
                    }
                    if (playableTracks.isEmpty()) {
                        emit(emptyList())
                        return@flow
                    }

                    val artists = playableTracks
                        .groupBy { (_, song) -> song.artistId }
                        .mapNotNull { (artistId, grouped) ->
                            val artist = catalogSource.getArtistById(artistId) ?: return@mapNotNull null
                            FavoritesRepository.LikedArtist(
                                artist = artist,
                                likedSongCount = grouped.size,
                                lastLikedAt = grouped.maxOf { it.first.addedAt },
                            )
                        }
                        .sortedByDescending { it.lastLikedAt }

                    emit(artists)
                }
            }

    override suspend fun toggle(songId: Song.Id): Boolean =
        if (favoritesStorage.isFavorite(songId)) {
            favoritesStorage.removeFavorite(songId)
            false
        } else {
            favoritesStorage.addFavorite(songId)
            true
        }

    override suspend fun setFavorite(songId: Song.Id, favorite: Boolean) {
        if (favorite) {
            favoritesStorage.addFavorite(songId)
        } else {
            favoritesStorage.removeFavorite(songId)
        }
    }

    override suspend fun toggleAlbum(artistId: Artist.Id, albumId: Album.Id): Boolean {
        val songIds = catalogSource.getSongsByAlbum(artistId, albumId).map(Song::id)
        if (songIds.isEmpty()) return false

        val currentFavorites = favoriteIds.first()
        val allLiked = songIds.all { it in currentFavorites }
        return if (allLiked) {
            favoritesStorage.removeFavorites(songIds)
            false
        } else {
            val toAdd = songIds.filter { it !in currentFavorites }
            favoritesStorage.addFavorites(toAdd)
            true
        }
    }

    override suspend fun toggleArtist(artistId: Artist.Id): Boolean {
        val songIds = catalogSource.getSongsByArtist(artistId).map(Song::id)
        if (songIds.isEmpty()) return false

        val currentFavorites = favoriteIds.first()
        val allLiked = songIds.all { it in currentFavorites }
        return if (allLiked) {
            favoritesStorage.removeFavorites(songIds)
            false
        } else {
            val toAdd = songIds.filter { it !in currentFavorites }
            favoritesStorage.addFavorites(toAdd)
            true
        }
    }

    override suspend fun resolvePlayableSongIds(): List<Song.Id> {
        val orderedSongIds = favoritesStorage.allOrdered.first().map { it.songId }
        if (orderedSongIds.isEmpty()) return emptyList()

        val resolvedSongIds = catalogSource.resolveSongsByIds(orderedSongIds.distinct())
            .associateBy { it.id }

        return orderedSongIds.mapNotNull { songId -> resolvedSongIds[songId]?.id }
    }
}
