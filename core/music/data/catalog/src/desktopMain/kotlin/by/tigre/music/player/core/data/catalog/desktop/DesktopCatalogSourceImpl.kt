package by.tigre.music.player.core.data.catalog.desktop

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class DesktopCatalogSourceImpl(dbDir: File) : CatalogSource {

    private val _dataVersion = MutableStateFlow(0L)
    override val dataVersion: Flow<Long> = _dataVersion.asStateFlow()

    private val connection: Connection by lazy {
        dbDir.mkdirs()
        val dbPath = File(dbDir, "catalog.db").absolutePath
        Class.forName("org.sqlite.JDBC")
        val conn = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        conn.createStatement().use { stmt ->
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS Artist (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    song_count INTEGER NOT NULL DEFAULT 0,
                    album_count INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS Album (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    artist_id INTEGER NOT NULL,
                    song_count INTEGER NOT NULL DEFAULT 0,
                    year_min INTEGER NOT NULL DEFAULT 0,
                    year_max INTEGER NOT NULL DEFAULT 0,
                    UNIQUE(name, artist_id)
                )
                """.trimIndent()
            )
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS Song (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    track_index TEXT NOT NULL DEFAULT '',
                    artist TEXT NOT NULL,
                    album TEXT NOT NULL,
                    artist_id INTEGER NOT NULL,
                    album_id INTEGER NOT NULL,
                    path TEXT NOT NULL UNIQUE
                )
                """.trimIndent()
            )
        }
        conn
    }

    override suspend fun getArtists(): List<Artist> {
        val result = mutableListOf<Artist>()
        connection.prepareStatement(
            "SELECT id, name, song_count, album_count FROM Artist ORDER BY name"
        ).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result.add(
                        Artist(
                            id = Artist.Id(rs.getLong(1)),
                            name = rs.getString(2),
                            songCount = rs.getInt(3),
                            albumCount = rs.getInt(4)
                        )
                    )
                }
            }
        }
        return result
    }

    override suspend fun getAlbums(artistId: Artist.Id): List<Album> {
        val result = mutableListOf<Album>()
        connection.prepareStatement(
            "SELECT id, name, song_count, year_min, year_max FROM Album WHERE artist_id = ? ORDER BY name"
        ).use { stmt ->
            stmt.setLong(1, artistId.value)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val yearMin = rs.getInt(4)
                    val yearMax = rs.getInt(5)
                    val years = when {
                        yearMin == 0 -> ""
                        yearMin == yearMax -> yearMin.toString()
                        else -> "$yearMin - $yearMax"
                    }
                    result.add(
                        Album(
                            id = Album.Id(rs.getLong(1)),
                            name = rs.getString(2),
                            songCount = rs.getInt(3),
                            years = years
                        )
                    )
                }
            }
        }
        return result
    }

    override suspend fun getSongsByArtist(artistId: Artist.Id): List<Song> {
        val result = mutableListOf<Song>()
        connection.prepareStatement(
            "SELECT id, name, track_index, artist, album, album_id, path FROM Song WHERE artist_id = ? ORDER BY album, track_index"
        ).use { stmt ->
            stmt.setLong(1, artistId.value)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result.add(mapRowToSong(rs))
                }
            }
        }
        return result
    }

    override suspend fun getSongsByAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song> {
        val result = mutableListOf<Song>()
        connection.prepareStatement(
            "SELECT id, name, track_index, artist, album, album_id, path FROM Song WHERE artist_id = ? AND album_id = ? ORDER BY track_index"
        ).use { stmt ->
            stmt.setLong(1, artistId.value)
            stmt.setLong(2, albumId.value)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result.add(mapRowToSong(rs))
                }
            }
        }
        return result
    }

    override suspend fun getSongsByIds(ids: List<Song.Id>): List<Song> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        val result = mutableListOf<Song>()
        connection.prepareStatement(
            "SELECT id, name, track_index, artist, album, album_id, path FROM Song WHERE id IN ($placeholders)"
        ).use { stmt ->
            ids.forEachIndexed { index, songId -> stmt.setLong(index + 1, songId.value) }
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result.add(mapRowToSong(rs))
                }
            }
        }
        return result
    }

    override suspend fun getSongById(id: Song.Id): Song? {
        connection.prepareStatement(
            "SELECT id, name, track_index, artist, album, album_id, path FROM Song WHERE id = ?"
        ).use { stmt ->
            stmt.setLong(1, id.value)
            stmt.executeQuery().use { rs ->
                if (rs.next()) return mapRowToSong(rs)
            }
        }
        return null
    }

    suspend fun addFolder(folder: File) {
        java.util.logging.Logger.getLogger("org.jaudiotagger").level = java.util.logging.Level.OFF

        val audioExtensions = setOf("mp3", "flac", "ogg", "m4a", "mp4", "wav", "aac", "wma")
        folder.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in audioExtensions }
            .forEach { file ->
                try {
                    val tag = AudioFileIO.read(file).tag
                    val title = tag?.getFirst(FieldKey.TITLE)?.takeIf { it.isNotBlank() }
                        ?: file.nameWithoutExtension
                    val artistName = tag?.getFirst(FieldKey.ARTIST)?.takeIf { it.isNotBlank() }
                        ?: "Unknown Artist"
                    val albumName = tag?.getFirst(FieldKey.ALBUM)?.takeIf { it.isNotBlank() }
                        ?: "Unknown Album"
                    val trackNo = tag?.getFirst(FieldKey.TRACK) ?: ""
                    val year = tag?.getFirst(FieldKey.YEAR)?.toIntOrNull() ?: 0

                    val artistId = getOrCreateArtist(artistName)
                    val albumId = getOrCreateAlbum(albumName, artistId, year)
                    insertOrUpdateSong(title, trackNo, artistName, albumName, artistId, albumId, file.absolutePath)
                } catch (e: Exception) {
                    // skip unreadable files
                }
            }

        updateCounts()
        _dataVersion.value++
    }

    private fun getOrCreateArtist(name: String): Long {
        connection.prepareStatement("SELECT id FROM Artist WHERE name = ?").use { stmt ->
            stmt.setString(1, name)
            stmt.executeQuery().use { rs -> if (rs.next()) return rs.getLong(1) }
        }
        connection.prepareStatement("INSERT INTO Artist(name) VALUES(?)").use { stmt ->
            stmt.setString(1, name)
            stmt.executeUpdate()
        }
        return lastInsertId()
    }

    private fun getOrCreateAlbum(name: String, artistId: Long, year: Int): Long {
        connection.prepareStatement(
            "SELECT id, year_min, year_max FROM Album WHERE name = ? AND artist_id = ?"
        ).use { stmt ->
            stmt.setString(1, name)
            stmt.setLong(2, artistId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    val id = rs.getLong(1)
                    if (year > 0) {
                        val yearMin = rs.getInt(2)
                        val yearMax = rs.getInt(3)
                        val newMin = if (yearMin == 0) year else minOf(yearMin, year)
                        val newMax = if (yearMax == 0) year else maxOf(yearMax, year)
                        connection.prepareStatement(
                            "UPDATE Album SET year_min = ?, year_max = ? WHERE id = ?"
                        ).use { updateStmt ->
                            updateStmt.setInt(1, newMin)
                            updateStmt.setInt(2, newMax)
                            updateStmt.setLong(3, id)
                            updateStmt.executeUpdate()
                        }
                    }
                    return id
                }
            }
        }
        connection.prepareStatement(
            "INSERT INTO Album(name, artist_id, year_min, year_max) VALUES(?, ?, ?, ?)"
        ).use { stmt ->
            stmt.setString(1, name)
            stmt.setLong(2, artistId)
            stmt.setInt(3, year)
            stmt.setInt(4, year)
            stmt.executeUpdate()
        }
        return lastInsertId()
    }

    private fun insertOrUpdateSong(
        title: String, trackNo: String, artist: String, album: String,
        artistId: Long, albumId: Long, path: String
    ) {
        connection.prepareStatement(
            "INSERT OR REPLACE INTO Song(name, track_index, artist, album, artist_id, album_id, path) VALUES(?, ?, ?, ?, ?, ?, ?)"
        ).use { stmt ->
            stmt.setString(1, title)
            stmt.setString(2, trackNo)
            stmt.setString(3, artist)
            stmt.setString(4, album)
            stmt.setLong(5, artistId)
            stmt.setLong(6, albumId)
            stmt.setString(7, path)
            stmt.executeUpdate()
        }
    }

    private fun updateCounts() {
        connection.createStatement().use { stmt ->
            stmt.executeUpdate(
                "UPDATE Artist SET song_count = (SELECT COUNT(*) FROM Song WHERE Song.artist_id = Artist.id), " +
                    "album_count = (SELECT COUNT(*) FROM Album WHERE Album.artist_id = Artist.id)"
            )
            stmt.executeUpdate(
                "UPDATE Album SET song_count = (SELECT COUNT(*) FROM Song WHERE Song.album_id = Album.id)"
            )
        }
    }

    private fun lastInsertId(): Long {
        connection.prepareStatement("SELECT last_insert_rowid()").use { stmt ->
            stmt.executeQuery().use { rs -> if (rs.next()) return rs.getLong(1) }
        }
        return -1L
    }

    private fun mapRowToSong(rs: java.sql.ResultSet): Song = Song(
        id = Song.Id(rs.getLong(1)),
        name = rs.getString(2),
        index = rs.getString(3),
        artist = rs.getString(4),
        album = rs.getString(5),
        albumId = Album.Id(rs.getLong(6)),
        path = rs.getString(7)
    )
}
