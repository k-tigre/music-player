package by.tigre.music.player.core.data.catalog.android

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.CatalogSearchResult
import by.tigre.music.player.core.entiry.catalog.Song

interface DbHelper {
    suspend fun getArtists(): List<Artist>
    suspend fun getArtistById(id: Artist.Id): Artist?
    suspend fun getAlbums(artistId: Artist.Id): List<Album>
    suspend fun getSongsByArtist(artistId: Artist.Id): List<Song>
    suspend fun getSongsByAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song>
    suspend fun getSongsByIds(ids: List<Song.Id>): List<Song>
    suspend fun getSongById(id: Song.Id): Song?
    suspend fun search(query: String): CatalogSearchResult
    suspend fun deleteSong(id: Song.Id): Boolean
    suspend fun deleteAlbum(artistId: Artist.Id, albumId: Album.Id): Boolean

    class Impl(private val context: Context) : DbHelper {

        override suspend fun getArtists(): List<Artist> {
            val projection = arrayOf(
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS
            )
            val artists = mutableListOf<Artist>()

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Artists.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Artists.getContentUri("external")
            }

            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                MediaStore.Audio.Artists.ARTIST + " ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
                val songCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)
                val albumCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
                while (cursor.moveToNext()) {
                    artists.add(
                        Artist(
                            id = Artist.Id(cursor.getLong(idColumn)),
                            name = cursor.getString(nameColumn),
                            songCount = cursor.getInt(songCountColumn),
                            albumCount = cursor.getInt(albumCountColumn)
                        )
                    )
                }
            }

            return artists
        }

        override suspend fun getArtistById(id: Artist.Id): Artist? {
            val projection = arrayOf(
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS
            )
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Artists.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Artists.getContentUri("external")
            }
            return context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Artists._ID} = ?",
                arrayOf(id.value.toString()),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    Artist(
                        id = Artist.Id(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID))),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)),
                        songCount = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)),
                        albumCount = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS))
                    )
                } else {
                    null
                }
            }
        }

        override suspend fun getAlbums(artistId: Artist.Id): List<Album> {
            val projection = arrayOf(
                MediaStore.Audio.Artists.Albums.ALBUM_ID,
                MediaStore.Audio.Artists.Albums.ALBUM,
                MediaStore.Audio.Artists.Albums.ALBUM_ART,
                MediaStore.Audio.Artists.Albums.NUMBER_OF_SONGS_FOR_ARTIST,
                MediaStore.Audio.Artists.Albums.FIRST_YEAR,
                MediaStore.Audio.Artists.Albums.LAST_YEAR
            )
            val albums = mutableListOf<Album>()

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Artists.Albums.getContentUri(MediaStore.VOLUME_EXTERNAL, artistId.value)
            } else {
                MediaStore.Audio.Artists.Albums.getContentUri("external", artistId.value)
            }

            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                MediaStore.Audio.Albums.FIRST_YEAR + " ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.Albums.ALBUM_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.Albums.ALBUM)
                val countColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.Albums.NUMBER_OF_SONGS_FOR_ARTIST)
                val firstYearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.Albums.FIRST_YEAR)
                val lastYearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.Albums.LAST_YEAR)
                while (cursor.moveToNext()) {
                    val yearStart = cursor.getString(firstYearColumn)
                    val yearEnd = cursor.getString(lastYearColumn)
                    val years = if (yearStart.isNullOrBlank().not() && yearEnd.isNullOrBlank()
                            .not()
                    ) "$yearStart - $yearEnd" else "$yearStart$yearEnd"
                    albums.add(
                        Album(
                            id = Album.Id(cursor.getLong(idColumn)),
                            name = cursor.getString(nameColumn),
                            songCount = cursor.getInt(countColumn),
                            years = years
                        )
                    )
                }
            }

            return albums
        }

        override suspend fun getSongsByArtist(artistId: Artist.Id): List<Song> =
            querySongs(
                selection = "${MediaStore.Audio.Media.IS_MUSIC} != ? AND ${MediaStore.Audio.Media.ARTIST_ID} = ?",
                selectionArgs = arrayOf("0", artistId.value.toString()),
                sortOrder = MediaStore.Audio.Media.TRACK + " ASC"
            )

        override suspend fun getSongById(id: Song.Id): Song? {
            val projection = songProjection()

            val collection = mediaCollection()

            return context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != ? AND ${MediaStore.Audio.Media._ID} == ?",
                arrayOf("0", id.value.toString()),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    mapSongRowFromCursor(cursor)
                } else {
                    null
                }
            }
        }

        override suspend fun getSongsByAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song> =
            querySongs(
                selection = "${MediaStore.Audio.Media.IS_MUSIC} != ? AND ${MediaStore.Audio.Media.ALBUM_ID} = ? AND ${MediaStore.Audio.Media.ARTIST_ID} = ?",
                selectionArgs = arrayOf("0", albumId.value.toString(), artistId.value.toString()),
                sortOrder = MediaStore.Audio.Media.TRACK + " ASC"
            )

        override suspend fun getSongsByIds(ids: List<Song.Id>): List<Song> {
            if (ids.isEmpty()) return emptyList()
            return querySongs(
                selection = "${MediaStore.Audio.Media.IS_MUSIC} != ? AND ${MediaStore.Audio.Media._ID} IN ${
                    ids.joinToString(separator = ",", postfix = ")", prefix = "(", transform = { it.value.toString() })
                }",
                selectionArgs = arrayOf("0"),
                sortOrder = null
            )
        }

        override suspend fun search(query: String): CatalogSearchResult {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return CatalogSearchResult(emptyList(), emptyList())
            val like = "%$trimmed%"
            val artists = mutableListOf<Artist>()
            val artistCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Artists.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Artists.getContentUri("external")
            }
            context.contentResolver.query(
                artistCollection,
                arrayOf(
                    MediaStore.Audio.Artists._ID,
                    MediaStore.Audio.Artists.ARTIST,
                    MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
                    MediaStore.Audio.Artists.NUMBER_OF_ALBUMS
                ),
                "${MediaStore.Audio.Artists.ARTIST} LIKE ?",
                arrayOf(like),
                MediaStore.Audio.Artists.ARTIST + " ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
                val songCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)
                val albumCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
                while (cursor.moveToNext()) {
                    artists.add(
                        Artist(
                            id = Artist.Id(cursor.getLong(idColumn)),
                            name = cursor.getString(nameColumn),
                            songCount = cursor.getInt(songCountColumn),
                            albumCount = cursor.getInt(albumCountColumn)
                        )
                    )
                }
            }
            val songs = querySongs(
                selection = "${MediaStore.Audio.Media.IS_MUSIC} != ? AND (${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?)",
                selectionArgs = arrayOf("0", like, like),
                sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
            )
            return CatalogSearchResult(artists = artists, songs = songs)
        }

        override suspend fun deleteSong(id: Song.Id): Boolean {
            val uri = ContentUris.withAppendedId(mediaCollection(), id.value)
            return deleteUris(listOf(uri))
        }

        override suspend fun deleteAlbum(artistId: Artist.Id, albumId: Album.Id): Boolean {
            val songs = getSongsByAlbum(artistId, albumId)
            if (songs.isEmpty()) return false
            val uris = songs.map { song -> ContentUris.withAppendedId(mediaCollection(), song.id.value) }
            return deleteUris(uris)
        }

        private suspend fun deleteUris(uris: List<Uri>): Boolean =
            MediaDeleteHandlerRegistry.delete(uris)

        private fun songProjection() = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.ALBUM_ID,
        )

        private fun mediaCollection() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.getContentUri("external")
        }

        private fun querySongs(
            selection: String,
            selectionArgs: Array<String>,
            sortOrder: String?
        ): List<Song> {
            val songs = mutableListOf<Song>()
            context.contentResolver.query(
                mediaCollection(),
                songProjection(),
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    songs.add(mapSongRowFromCursor(cursor))
                }
            }
            return songs
        }

        private fun mapSongRowFromCursor(cursor: android.database.Cursor): Song = mapSongRow(
            cursor = cursor,
            idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID),
            nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE),
            trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK),
            albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM),
            artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST),
            dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA),
            albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID),
            artistIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
        )

        private fun mapSongRow(
            cursor: android.database.Cursor,
            idColumn: Int,
            nameColumn: Int,
            trackColumn: Int,
            albumColumn: Int,
            artistColumn: Int,
            dataColumn: Int,
            albumIdColumn: Int,
            artistIdColumn: Int
        ): Song = Song(
            id = Song.Id(cursor.getLong(idColumn)),
            name = cursor.getString(nameColumn),
            index = cursor.getString(trackColumn) ?: "",
            album = cursor.getString(albumColumn),
            artist = cursor.getString(artistColumn),
            path = cursor.getString(dataColumn),
            artistId = Artist.Id(cursor.getLong(artistIdColumn)),
            albumId = Album.Id(cursor.getLong(albumIdColumn))
        )
    }
}
