package by.tigre.media.platform.background.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import by.tigre.logger.Log
import by.tigre.media.platform.tools.platform.utils.CoverArtCache
import java.io.File

/**
 * Widget-sized artwork cache + helpers for MediaSession bytes.
 * Decoding/materialization for UI lives in [CoverArtCache].
 */
object WidgetArtworkCache {

    private const val FILE_NAME = "playback_widget_artwork.png"
    private const val WIDGET_MAX_SIZE_PX = 256

    fun materialize(context: Context, uri: Uri?, maxSizePx: Int = 1024): File? =
        CoverArtCache.materialize(context, uri, maxSizePx)

    fun materializeEmbedded(context: Context, audioUri: Uri?, maxSizePx: Int = 1024): File? =
        CoverArtCache.materializeEmbedded(context, audioUri, maxSizePx)

    fun update(context: Context, uri: Uri?) {
        val file = cacheFile(context)
        if (uri == null) {
            file.delete()
            return
        }
        try {
            val decoded = CoverArtCache.decodeBitmap(context, uri, WIDGET_MAX_SIZE_PX)
            if (decoded == null) {
                file.delete()
                return
            }
            val scaled = scaleDown(decoded, WIDGET_MAX_SIZE_PX)
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

    fun loadBytes(context: Context): ByteArray? {
        val file = cacheFile(context)
        if (!file.exists()) return null
        return try {
            file.readBytes()
        } catch (e: Exception) {
            Log.e("PlaybackWidget") { "Failed to read widget artwork bytes: $e" }
            null
        }
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
