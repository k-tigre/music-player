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
    override suspend fun getAlbums(artistId: Long): List<Album> = dbHelper.getAlbums(artistId)
    override suspend fun getSongsByArtist(artistId: Long): List<Song> = dbHelper.getSongsByArtist(artistId)
    override suspend fun getSongsByAlbum(albumId: Long): List<Song> = dbHelper.getSongsByAlbum(albumId)
    override suspend fun getSongById(id: Long): Song? = dbHelper.getSongById(id)
}