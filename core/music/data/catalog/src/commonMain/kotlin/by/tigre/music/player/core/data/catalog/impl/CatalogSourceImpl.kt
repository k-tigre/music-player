package by.tigre.music.player.core.data.catalog.impl

import by.tigre.music.player.core.data.catalog.CatalogBackend
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.catalog.hidden.HiddenCatalogStorage
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.CatalogSearchResult
import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

internal class CatalogSourceImpl(
    private val backend: CatalogBackend,
    private val hidden: HiddenCatalogStorage,
) : CatalogSource {

    private val _dataVersion = MutableStateFlow(0L)
    override val dataVersion: Flow<Long> = combine(
        _dataVersion,
        hidden.revision,
        backend.dataRevision
    ) { local, hiddenRev, backendRev -> local + hiddenRev + backendRev }

    override suspend fun getArtists(): List<Artist> =
        backend.getArtists().filter { artist -> getAlbums(artist.id).isNotEmpty() }

    override suspend fun getArtistById(id: Artist.Id): Artist? {
        val artist = backend.getArtistById(id) ?: return null
        return if (getAlbums(id).isEmpty()) null else artist
    }

    override suspend fun getAlbums(artistId: Artist.Id): List<Album> =
        backend.getAlbums(artistId).filter { album -> !hidden.isAlbumHidden(artistId, album.id) }

    override suspend fun getSongsByArtist(artistId: Artist.Id): List<Song> =
        backend.getSongsByArtist(artistId).filterVisible()

    override suspend fun getSongsByAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song> {
        if (hidden.isAlbumHidden(artistId, albumId)) return emptyList()
        return backend.getSongsByAlbum(artistId, albumId).filterVisible()
    }

    override suspend fun getSongById(id: Song.Id): Song? {
        val song = backend.getSongById(id) ?: return null
        return if (hidden.isSongHidden(id)) null else song
    }

    override suspend fun getSongsByIds(ids: List<Song.Id>): List<Song> =
        backend.getSongsByIds(ids).filterVisible()

    override suspend fun search(query: String): CatalogSearchResult {
        val raw = backend.search(query)
        return CatalogSearchResult(
            artists = raw.artists.filter { artist -> getAlbums(artist.id).isNotEmpty() },
            songs = raw.songs.filterVisible()
        )
    }

    override suspend fun hideSong(id: Song.Id) {
        hidden.hideSong(id)
    }

    override suspend fun hideAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song.Id> {
        val songs = backend.getSongsByAlbum(artistId, albumId)
        val songIds = songs.map(Song::id)
        hidden.hideAlbum(artistId, albumId, songIds)
        return songIds
    }

    override suspend fun deleteSong(id: Song.Id): Boolean {
        val deleted = backend.deleteSong(id)
        if (deleted) bump()
        return deleted
    }

    override suspend fun deleteAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song.Id> {
        val songIds = backend.getSongsByAlbum(artistId, albumId).map(Song::id)
        if (backend.deleteAlbum(artistId, albumId)) {
            bump()
            return songIds
        }
        return emptyList()
    }

    private fun List<Song>.filterVisible(): List<Song> =
        filter { song -> !hidden.isSongHidden(song.id) }

    private fun bump() {
        _dataVersion.value++
    }
}
