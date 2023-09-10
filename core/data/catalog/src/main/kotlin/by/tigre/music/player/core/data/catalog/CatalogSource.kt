package by.tigre.music.player.core.data.catalog

import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song

interface CatalogSource {
    suspend fun getArtists(): List<Artist>
    suspend fun getAlbums(artistId: Artist.Id): List<Album>
    suspend fun getSongsByArtist(artistId: Artist.Id): List<Song>
    suspend fun getSongsByAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song>
    suspend fun getSongsByIds(ids: List<Song.Id>): List<Song>
    suspend fun getSongById(id: Song.Id): Song?
}
