package by.tigre.music.player.core.data.catalog.android

import by.tigre.music.player.core.data.catalog.CatalogBackend
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.CatalogSearchResult
import by.tigre.music.player.core.entiry.catalog.Song

internal class AndroidCatalogBackend(
    private val dbHelper: DbHelper,
) : CatalogBackend {

    override suspend fun getArtists(): List<Artist> = dbHelper.getArtists()
    override suspend fun getArtistById(id: Artist.Id): Artist? = dbHelper.getArtistById(id)
    override suspend fun getAlbums(artistId: Artist.Id): List<Album> = dbHelper.getAlbums(artistId)
    override suspend fun getSongsByArtist(artistId: Artist.Id): List<Song> = dbHelper.getSongsByArtist(artistId)
    override suspend fun getSongsByAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song> =
        dbHelper.getSongsByAlbum(artistId, albumId)
    override suspend fun getSongById(id: Song.Id): Song? = dbHelper.getSongById(id)
    override suspend fun getSongsByIds(ids: List<Song.Id>): List<Song> = dbHelper.getSongsByIds(ids)
    override suspend fun search(query: String): CatalogSearchResult = dbHelper.search(query)
    override suspend fun deleteSong(id: Song.Id): Boolean = dbHelper.deleteSong(id)
    override suspend fun deleteAlbum(artistId: Artist.Id, albumId: Album.Id): Boolean =
        dbHelper.deleteAlbum(artistId, albumId)
}
