package by.tigre.media.platform.background.widget

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import by.tigre.logger.Log
import java.io.File

internal object WidgetArtworkCache {

    private const val FILE_NAME = "playback_widget_artwork.png"
    private const val MAX_SIZE_PX = 256
    private val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")

    fun update(context: Context, uri: Uri?) {
        val file = cacheFile(context)
        if (uri == null) {
            file.delete()
            return
        }
        try {
            val decoded = loadBitmap(context, uri)
            if (decoded == null) {
                file.delete()
                return
            }
            val scaled = scaleDown(decoded, MAX_SIZE_PX)
            file.outputStream().use { output ->
                scaled.compress(Bitmap.CompressFormat.PNG, 90, output)
            }
            if (scaled !== decoded) {
                decoded.recycle()
            }
        } catch (e: Exception) {
            Log.e("PlaybackWidget") { "Failed to cache widget artwork: $e" }
            file.delete()
        }
    }

    fun load(context: Context): Bitmap? {
        val file = cacheFile(context)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            Log.e("PlaybackWidget") { "Failed to load widget artwork: $e" }
            null
        }
    }

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        val artworkUri = resolveArtworkUri(uri)
        try {
            context.contentResolver.openInputStream(artworkUri)?.use { input ->
                BitmapFactory.decodeStream(input)?.let { return it }
            }
        } catch (e: Exception) {
            Log.e("PlaybackWidget") { "Failed to open artwork stream for $artworkUri: $e" }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                return context.contentResolver.loadThumbnail(
                    artworkUri,
                    Size(MAX_SIZE_PX, MAX_SIZE_PX),
                    null,
                )
            } catch (e: Exception) {
                Log.e("PlaybackWidget") { "Failed to load artwork thumbnail for $artworkUri: $e" }
            }
        }
        return null
    }

    private fun resolveArtworkUri(uri: Uri): Uri {
        val path = uri.path.orEmpty()
        if (path.contains("/audio/albums/")) {
            val albumId = ContentUris.parseId(uri)
            return ContentUris.withAppendedId(ALBUM_ART_URI, albumId)
        }
        return uri
    }

    private fun cacheFile(context: Context): File {
        return File(context.applicationContext.cacheDir, FILE_NAME)
    }

    private fun scaleDown(source: Bitmap, maxSizePx: Int): Bitmap {
        val largestSide = maxOf(source.width, source.height)
        if (largestSide <= maxSizePx) return source
        val scale = maxSizePx.toFloat() / largestSide
        val width = (source.width * scale).toInt().coerceAtLeast(1)
        val height = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }
}
