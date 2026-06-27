package by.tigre.music.player.core.data.catalog.desktop

import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.entiry.catalog.Album
import org.jaudiotagger.audio.AudioFileIO
import java.io.File

class DesktopAlbumArtProvider(
    private val cacheDir: File,
    private val firstSongPathForAlbum: (Album.Id) -> String?,
) : AlbumArtProvider {

    override fun albumArtUri(albumId: Album.Id): Any? {
        val cacheFile = File(cacheDir, "album_${albumId.value}.jpg")
        if (cacheFile.exists()) return cacheFile.absolutePath

        val songPath = firstSongPathForAlbum(albumId) ?: return null
        if (!extractEmbeddedArt(songPath, cacheFile)) return null
        return cacheFile.absolutePath
    }

    private fun extractEmbeddedArt(sourcePath: String, destFile: File): Boolean {
        return try {
            val artwork = AudioFileIO.read(File(sourcePath)).tag?.firstArtwork ?: return false
            val data = artwork.binaryData ?: return false
            cacheDir.mkdirs()
            destFile.writeBytes(data)
            true
        } catch (_: Exception) {
            false
        }
    }
}
