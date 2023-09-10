package by.tigre.music.player.core.data.catalog.impl

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.catalog.android.DbHelper
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song

internal class CatalogSourceImpl(
    private val dbHelper: DbHelper,
) : CatalogSource {

    override suspend fun getArtists(): List<Artist> = dbHelper.getArtists()
    override suspend fun getAlbums(artistId: Artist.Id): List<Album> = dbHelper.getAlbums(artistId)
    override suspend fun getSongsByArtist(artistId: Artist.Id): List<Song> = dbHelper.getSongsByArtist(artistId)
    override suspend fun getSongsByAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song> = dbHelper.getSongsByAlbum(artistId, albumId)
    override suspend fun getSongById(id: Song.Id): Song? = dbHelper.getSongById(id)
    override suspend fun getSongsByIds(ids: List<Song.Id>): List<Song> = dbHelper.getSongsByIds(ids)
}
