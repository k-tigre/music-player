package by.tigre.music.player.core.data.catalog

import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song

interface CatalogSource {
    suspend fun getArtists(): List<Artist>
    suspend fun getAlbums(artistId: Long): List<Album>
    suspend fun getSongsByArtist(artistId: Long): List<Song>
    suspend fun getSongsByAlbum(albumId: Long): List<Song>
    suspend fun getSongById(id: Long): Song?
}
