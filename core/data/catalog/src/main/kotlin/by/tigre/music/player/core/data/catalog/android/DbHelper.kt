package by.tigre.music.player.core.data.catalog.android

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song

interface DbHelper {
    suspend fun getArtists(): List<Artist>
    suspend fun getAlbums(artistId: Artist.Id): List<Album>
    suspend fun getSongsByArtist(artistId: Artist.Id): List<Song>
    suspend fun getSongsByAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song>
    suspend fun getSongsByIds(ids: List<Song.Id>): List<Song>
    suspend fun getSongById(id: Song.Id): Song?

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
                println("!!!!! !!!!! cursor count = ${cursor.count}")
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

        override suspend fun getAlbums(artistId: Artist.Id): List<Album> {
            val projection = arrayOf(
                MediaStore.Audio.Artists.Albums.ALBUM_ID,
                MediaStore.Audio.Artists.Albums.ALBUM,
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
                println("!!!!! !!!!! cursor count = ${cursor.count}")
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.Albums.ALBUM_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.Albums.ALBUM)
                val countColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.Albums.NUMBER_OF_SONGS_FOR_ARTIST)
                val firstYearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.Albums.FIRST_YEAR)
                val lastYearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.Albums.LAST_YEAR)
                while (cursor.moveToNext()) {
                    val yearStart = cursor.getString(firstYearColumn)
                    val yearEnd = cursor.getString(lastYearColumn)
                    val years = if (yearStart.isNotBlank() && yearEnd.isNotBlank()) "$yearStart - $yearEnd" else "$yearStart$yearEnd"
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

        override suspend fun getSongsByArtist(artistId: Artist.Id): List<Song> {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TRACK
            )

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.getContentUri("external")
            }

            val songs = mutableListOf<Song>()

            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != ? AND ${MediaStore.Audio.Media.ARTIST_ID} == ?",
                arrayOf("0", artistId.value.toString()),
                MediaStore.Audio.Media.TRACK + " ASC"
            )?.use { cursor ->
                println("!!!!! !!!!! cursor count = ${cursor.count}")
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                while (cursor.moveToNext()) {
                    songs.add(
                        Song(
                            id = Song.Id(cursor.getLong(idColumn)),
                            name = cursor.getString(nameColumn),
                            index = cursor.getString(trackColumn) ?: "",
                            album = cursor.getString(albumColumn),
                            artist = cursor.getString(artistColumn),
                            path = cursor.getString(dataColumn)
                        )
                    )
                }
            }
            return songs
        }

        override suspend fun getSongById(id: Song.Id): Song? {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TRACK
            )

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.getContentUri("external")
            }

            return context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != ? AND ${MediaStore.Audio.Media._ID} == ?",
                arrayOf("0", id.value.toString()),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    Song(
                        id = Song.Id(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)),
                        album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)),
                        artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)),
                        path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)),
                        index = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK))
                    )
                } else {
                    null
                }
            }
        }

        override suspend fun getSongsByAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song> {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DATA,
            )
            val songs = mutableListOf<Song>()

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.getContentUri("external")
            }

            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != ? AND ${MediaStore.Audio.Media.ALBUM_ID} == ? AND ${MediaStore.Audio.Media.ARTIST_ID} == ?",
                arrayOf("0", albumId.value.toString(), artistId.value.toString()),
                MediaStore.Audio.Media.TRACK + " ASC"
            )?.use { cursor ->
                println("!!!!! !!!!! cursor count = ${cursor.count}")
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                while (cursor.moveToNext()) {
                    songs.add(
                        Song(
                            id = Song.Id(cursor.getLong(idColumn)),
                            name = cursor.getString(nameColumn),
                            index = cursor.getString(trackColumn),
                            album = cursor.getString(albumColumn),
                            artist = cursor.getString(artistColumn),
                            path = cursor.getString(dataColumn)
                        )
                    )
                }
            }
            return songs
        }

        override suspend fun getSongsByIds(ids: List<Song.Id>): List<Song> {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DATA,
            )
            val songs = mutableListOf<Song>()

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.getContentUri("external")
            }

            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != ? AND ${MediaStore.Audio.Media._ID} IN ${
                    ids.joinToString(
                        separator = ",",
                        postfix = ")",
                        prefix = "(",
                        transform = { it.value.toString() }
                    )
                }",
                arrayOf("0"),
                null
            )?.use { cursor ->
                println("!!!!! !!!!! cursor count = ${cursor.count}")
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                while (cursor.moveToNext()) {
                    songs.add(
                        Song(
                            id = Song.Id(cursor.getLong(idColumn)),
                            name = cursor.getString(nameColumn),
                            index = cursor.getString(trackColumn),
                            album = cursor.getString(albumColumn),
                            artist = cursor.getString(artistColumn),
                            path = cursor.getString(dataColumn)
                        )
                    )
                }
            }
            return songs
        }
    }
}
