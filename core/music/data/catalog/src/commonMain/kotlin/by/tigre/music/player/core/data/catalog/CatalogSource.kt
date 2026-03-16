package by.tigre.music.player.core.data.catalog

import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface CatalogSource {
    val dataVersion: Flow<Long> get() = flowOf(0L)
    suspend fun getArtists(): List<Artist>
    suspend fun getAlbums(artistId: Artist.Id): List<Album>
    suspend fun getSongsByArtist(artistId: Artist.Id): List<Song>
    suspend fun getSongsByAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song>
    suspend fun getSongsByIds(ids: List<Song.Id>): List<Song>
    suspend fun getSongById(id: Song.Id): Song?
}
