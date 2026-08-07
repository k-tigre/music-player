package by.tigre.music.player.core.data.catalog.impl

import by.tigre.music.player.core.data.catalog.CatalogBackend
import by.tigre.music.player.core.data.catalog.hidden.HiddenCatalogStorage
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.CatalogSearchResult
import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogSourceImplTest {

    @Test
    fun getArtistsDoesNotQueryAlbumsWhenNothingIsHidden() = runBlocking {
        val backend = FakeBackend(
            artists = listOf(
                artist(1, "A", albums = 2),
                artist(2, "B", albums = 1),
                artist(3, "C", albums = 3),
            ),
            albumsByArtist = mapOf(
                Artist.Id(1) to listOf(album(10), album(11)),
                Artist.Id(2) to listOf(album(20)),
                Artist.Id(3) to listOf(album(30), album(31), album(32)),
            ),
        )
        val source = CatalogSourceImpl(backend = backend, hidden = FakeHidden())

        val artists = source.getArtists()

        assertEquals(3, artists.size)
        assertEquals(0, backend.getAlbumsCalls)
    }

    @Test
    fun getArtistsQueriesAlbumsOnlyForArtistsWithHiddenAlbums() = runBlocking {
        val artistWithHidden = Artist.Id(2)
        val backend = FakeBackend(
            artists = listOf(
                artist(1, "A", albums = 2),
                artist(2, "B", albums = 1),
                artist(3, "C", albums = 3),
            ),
            albumsByArtist = mapOf(
                Artist.Id(1) to listOf(album(10), album(11)),
                artistWithHidden to listOf(album(20)),
                Artist.Id(3) to listOf(album(30), album(31), album(32)),
            ),
        )
        val hidden = FakeHidden(
            hiddenAlbums = setOf(artistWithHidden to Album.Id(20)),
        )
        val source = CatalogSourceImpl(backend = backend, hidden = hidden)

        val artists = source.getArtists()

        assertEquals(listOf(Artist.Id(1), Artist.Id(3)), artists.map { it.id })
        assertEquals(1, backend.getAlbumsCalls)
        assertEquals(listOf(artistWithHidden), backend.getAlbumsCallIds)
    }

    @Test
    fun searchDoesNotQueryAlbumsWhenNothingIsHidden() = runBlocking {
        val backend = FakeBackend(
            artists = listOf(artist(1, "Alpha", albums = 1)),
            albumsByArtist = mapOf(Artist.Id(1) to listOf(album(10))),
            searchResult = CatalogSearchResult(
                artists = listOf(artist(1, "Alpha", albums = 1)),
                songs = emptyList(),
            ),
        )
        val source = CatalogSourceImpl(backend = backend, hidden = FakeHidden())

        val result = source.search("Al")

        assertEquals(1, result.artists.size)
        assertEquals(0, backend.getAlbumsCalls)
    }

    private fun artist(id: Long, name: String, albums: Int) = Artist(
        id = Artist.Id(id),
        name = name,
        songCount = albums,
        albumCount = albums,
    )

    private fun album(id: Long) = Album(
        id = Album.Id(id),
        name = "Album $id",
        songCount = 1,
        years = "2020",
    )

    private class FakeHidden(
        private val hiddenAlbums: Set<Pair<Artist.Id, Album.Id>> = emptySet(),
        private val hiddenSongs: Set<Song.Id> = emptySet(),
    ) : HiddenCatalogStorage {
        private val _revision = MutableStateFlow(0L)
        override val revision: Flow<Long> = _revision.asStateFlow()

        override fun isSongHidden(id: Song.Id): Boolean = id in hiddenSongs

        override fun isAlbumHidden(artistId: Artist.Id, albumId: Album.Id): Boolean =
            (artistId to albumId) in hiddenAlbums

        override fun artistIdsWithHiddenAlbums(): Set<Artist.Id> =
            hiddenAlbums.map { it.first }.toSet()

        override fun hideSong(id: Song.Id) = Unit

        override fun hideAlbum(artistId: Artist.Id, albumId: Album.Id, songIds: List<Song.Id>) = Unit
    }

    private class FakeBackend(
        private val artists: List<Artist>,
        private val albumsByArtist: Map<Artist.Id, List<Album>>,
        private val searchResult: CatalogSearchResult = CatalogSearchResult(emptyList(), emptyList()),
    ) : CatalogBackend {
        var getAlbumsCalls: Int = 0
            private set
        val getAlbumsCallIds = mutableListOf<Artist.Id>()

        override suspend fun getArtists(): List<Artist> = artists

        override suspend fun getArtistById(id: Artist.Id): Artist? = artists.find { it.id == id }

        override suspend fun getAlbums(artistId: Artist.Id): List<Album> {
            getAlbumsCalls++
            getAlbumsCallIds += artistId
            return albumsByArtist[artistId].orEmpty()
        }

        override suspend fun getSongsByArtist(artistId: Artist.Id): List<Song> = emptyList()
        override suspend fun getSongsByAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song> = emptyList()
        override suspend fun getSongsByIds(ids: List<Song.Id>): List<Song> = emptyList()
        override suspend fun getSongById(id: Song.Id): Song? = null
        override suspend fun search(query: String): CatalogSearchResult = searchResult
        override suspend fun deleteSong(id: Song.Id): Boolean = false
        override suspend fun deleteAlbum(artistId: Artist.Id, albumId: Album.Id): Boolean = false
    }
}
