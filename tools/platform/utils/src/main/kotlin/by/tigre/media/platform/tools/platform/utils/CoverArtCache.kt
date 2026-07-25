package by.tigre.media.platform.tools.platform.utils

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import java.io.File
import java.security.MessageDigest

/**
 * Decodes cover art the way System UI / MediaSession can (stream + [loadThumbnail] fallback),
 * then materializes to a local PNG/JPEG so Coil can display it reliably.
 *
 * Coil often fails on SAF `content://` tree URIs and HEIC; BitmapFactory/`loadThumbnail` succeed.
 */
object CoverArtCache {

    private const val TAG = "CoverArt"
    private const val COVERS_DIR = "cover_art_cache"
    private const val EMBEDDED_DIR = "cover_art_embedded"
    private const val DEFAULT_MAX_SIZE_PX = 1024
    private val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")

    /**
     * For Coil/UI: turn SAF/content URIs into a local [File]; leave other models as-is.
     */
    fun resolveForDisplay(context: Context, model: Any?, maxSizePx: Int = DEFAULT_MAX_SIZE_PX): Any? {
        val uri = when (model) {
            null -> return null
            is Uri -> model
            is String -> {
                if (!model.startsWith("content:", ignoreCase = true) &&
                    !model.startsWith("file:", ignoreCase = true)
                ) {
                    return model
                }
                Uri.parse(model)
            }
            is File -> return model
            else -> return model
        }
        if (uri.scheme == "file") {
            val path = uri.path ?: return model
            val file = File(path)
            return if (file.exists()) file else model
        }
        if (uri.scheme != "content") return model
        val file = materialize(context, uri, maxSizePx)
        if (file != null) {
            Log.i(TAG, "resolveForDisplay ok uri=$uri → ${file.absolutePath} (${file.length()} bytes)")
        } else {
            Log.w(TAG, "resolveForDisplay FAILED uri=$uri")
        }
        return file ?: model
    }

    /**
     * Decodes [uri] into a stable local PNG. Returns null if decoding fails.
     */
    fun materialize(context: Context, uri: Uri?, maxSizePx: Int = DEFAULT_MAX_SIZE_PX): File? {
        if (uri == null) {
            Log.d(TAG, "materialize: uri=null")
            return null
        }
        val file = perSourceFile(context, COVERS_DIR, uri, "png")
        if (file.exists() && file.length() > 0L) {
            Log.d(TAG, "materialize cache hit ${file.name} for $uri")
            return file
        }
        return try {
            val decoded = decodeBitmap(context, uri, maxSizePx)
            if (decoded == null) {
                Log.w(TAG, "materialize: decodeBitmap returned null for $uri")
                return null
            }
            val scaled = scaleDown(decoded, maxSizePx)
            file.parentFile?.mkdirs()
            file.outputStream().use { output ->
                scaled.compress(Bitmap.CompressFormat.PNG, 90, output)
            }
            if (scaled !== decoded) {
                decoded.recycle()
            }
            Log.i(TAG, "materialize wrote ${file.absolutePath} (${file.length()} bytes) from $uri")
            file
        } catch (e: Exception) {
            Log.e(TAG, "materialize failed for $uri", e)
            file.delete()
            null
        }
    }

    /**
     * Reads embedded album art from an audio file and caches it as JPEG.
     */
    fun materializeEmbedded(context: Context, audioUri: Uri?, maxSizePx: Int = DEFAULT_MAX_SIZE_PX): File? {
        if (audioUri == null) {
            Log.d(TAG, "materializeEmbedded: audioUri=null")
            return null
        }
        val file = perSourceFile(context, EMBEDDED_DIR, audioUri, "jpg")
        if (file.exists() && file.length() > 0L) {
            Log.d(TAG, "materializeEmbedded cache hit ${file.name} for $audioUri")
            return file
        }
        val bytes = readEmbeddedPicture(context, audioUri) ?: return null
        return try {
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (decoded == null) {
                file.parentFile?.mkdirs()
                file.writeBytes(bytes)
                Log.i(TAG, "materializeEmbedded raw write ${file.absolutePath} (${bytes.size} bytes)")
                return file
            }
            val scaled = scaleDown(decoded, maxSizePx)
            file.parentFile?.mkdirs()
            file.outputStream().use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            if (scaled !== decoded) {
                decoded.recycle()
            } else {
                decoded.recycle()
            }
            Log.i(TAG, "materializeEmbedded wrote ${file.absolutePath} (${file.length()} bytes) from $audioUri")
            file
        } catch (e: Exception) {
            Log.e(TAG, "materializeEmbedded failed for $audioUri", e)
            file.delete()
            null
        }
    }

    fun decodeBitmap(context: Context, uri: Uri, maxSizePx: Int = DEFAULT_MAX_SIZE_PX): Bitmap? {
        val artworkUri = resolveArtworkUri(uri)
        try {
            context.contentResolver.openInputStream(artworkUri)?.use { input ->
                BitmapFactory.decodeStream(input)?.let {
                    Log.d(TAG, "decodeBitmap via stream ${it.width}x${it.height} $artworkUri")
                    return it
                }
            }
            Log.d(TAG, "decodeBitmap stream produced null for $artworkUri")
        } catch (e: Exception) {
            Log.w(TAG, "decodeBitmap openInputStream failed for $artworkUri: ${e.message}")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val thumb = context.contentResolver.loadThumbnail(
                    artworkUri,
                    Size(maxSizePx, maxSizePx),
                    null,
                )
                Log.d(TAG, "decodeBitmap via loadThumbnail ${thumb.width}x${thumb.height} $artworkUri")
                return thumb
            } catch (e: Exception) {
                Log.w(TAG, "decodeBitmap loadThumbnail failed for $artworkUri: ${e.message}")
            }
        }
        return null
    }

    fun readEmbeddedPicture(context: Context, audioUri: Uri): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, audioUri)
            val picture = retriever.embeddedPicture
            if (picture == null) {
                Log.d(TAG, "readEmbeddedPicture: none in $audioUri")
            } else {
                Log.i(TAG, "readEmbeddedPicture: ${picture.size} bytes from $audioUri")
            }
            picture
        } catch (e: Exception) {
            Log.w(TAG, "readEmbeddedPicture failed for $audioUri: ${e.message}")
            null
        } finally {
            retriever.release()
        }
    }

    private fun resolveArtworkUri(uri: Uri): Uri {
        val path = uri.path.orEmpty()
        if (path.contains("/audio/albums/")) {
            val albumId = ContentUris.parseId(uri)
            return ContentUris.withAppendedId(ALBUM_ART_URI, albumId)
        }
        return uri
    }

    private fun perSourceFile(context: Context, dirName: String, uri: Uri, ext: String): File {
        val key = sha256Hex(uri.toString()).take(32)
        // filesDir so paths stored in DB survive cache clears
        return File(context.applicationContext.filesDir, "$dirName/$key.$ext")
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { b -> "%02x".format(b) }
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
