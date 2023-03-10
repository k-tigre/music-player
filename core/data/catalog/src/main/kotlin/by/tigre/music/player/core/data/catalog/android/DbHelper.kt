package by.tigre.music.player.core.data.catalog.android

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import java.io.File

interface DbHelper {
    suspend fun getArtists(): List<Artist>
    suspend fun getAlbums(artistId: Long): List<Album>
    suspend fun getSongsByArtist(artistId: Long): List<Song>
    suspend fun getSongsByAlbum(albumId: Long): List<Song>

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
                            id = cursor.getLong(idColumn),
                            name = cursor.getString(nameColumn),
                            songCount = cursor.getInt(songCountColumn),
                            albumCount = cursor.getInt(albumCountColumn)
                        )
                    )
                }
            }

            return artists
        }

        override suspend fun getAlbums(artistId: Long): List<Album> {
            val projection = arrayOf(
                MediaStore.Audio.Artists.Albums._ID,
                MediaStore.Audio.Artists.Albums.ALBUM,
                MediaStore.Audio.Artists.Albums.NUMBER_OF_SONGS,
                MediaStore.Audio.Artists.Albums.FIRST_YEAR,
                MediaStore.Audio.Artists.Albums.LAST_YEAR
            )
            val albums = mutableListOf<Album>()

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Artists.Albums.getContentUri(MediaStore.VOLUME_EXTERNAL, artistId)
            } else {
                MediaStore.Audio.Artists.Albums.getContentUri("external", artistId)
            }

            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                MediaStore.Audio.Albums.FIRST_YEAR + " ASC"
            )?.use { cursor ->
                println("!!!!! !!!!! cursor count = ${cursor.count}")
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.Albums._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.Albums.ALBUM)
                val countColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.Albums.NUMBER_OF_SONGS)
                val firstYearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.Albums.FIRST_YEAR)
                val lastYearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.Albums.LAST_YEAR)
                while (cursor.moveToNext()) {
                    albums.add(
                        Album(
                            id = cursor.getLong(idColumn),
                            name = cursor.getString(nameColumn),
                            songCount = cursor.getInt(countColumn),
                            years = "${cursor.getString(firstYearColumn)} - ${cursor.getString(lastYearColumn)}"
                        )
                    )
                }
            }

            return albums
        }

        override suspend fun getSongsByArtist(artistId: Long): List<Song> {
            TODO("Not yet implemented")
        }

        override suspend fun getSongsByAlbum(albumId: Long): List<Song> {
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
                "${MediaStore.Audio.Media.IS_MUSIC} != ? AND ${MediaStore.Audio.Media.ALBUM_ID} == ?",
                arrayOf("0", albumId.toString()),
                MediaStore.Audio.Media.TRACK + " ASC"
            )?.use { cursor ->
                println("!!!!! !!!!! cursor count = ${cursor.count}")
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val dColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                while (cursor.moveToNext()) {
                    println(
                        "!!!!! !!!!! cursor album=$albumId, id=${cursor.getLong(idColumn)}  --TRACK=${cursor.getString(trackColumn)} -" +
                                "- ARTIST=${cursor.getString(artistColumn)} --ALBUM=${cursor.getString(albumColumn)} -" +
                                "- name=${cursor.getString(nameColumn)} -- DISPLAY_NAME=${cursor.getString(dColumn)} --" +
                                "DATA=${cursor.getString(dataColumn)} -- ${File(cursor.getString(dataColumn)).run { "$name--$parent -- ${exists()} -- ${canRead()}" }}"
                    )
                    songs.add(
                        Song(
                            id = cursor.getLong(idColumn),
                            name = "${cursor.getString(trackColumn)} - ${cursor.getString(nameColumn)}",
                            album = cursor.getString(albumColumn),
                            artist = cursor.getString(artistColumn)
                        )
                    )
                }
            }
            return songs
        }
    }
}
