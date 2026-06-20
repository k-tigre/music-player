package by.tigre.music.player.core.data.catalog.android

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.entiry.catalog.Song

object ExternalAudioUriResolver {

    suspend fun resolveSongId(context: Context, uri: Uri, catalog: CatalogSource): Song.Id? {
        if (uri.scheme == ContentResolverScheme.CONTENT && uri.authority?.contains("media") == true) {
            val mediaId = runCatching { ContentUris.parseId(uri) }.getOrNull()
            if (mediaId != null && mediaId > 0L) {
                catalog.getSongById(Song.Id(mediaId))?.let { return it.id }
            }
        }
        return null
    }

    fun resolveDisplayTitle(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    cursor.getString(index)?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: uri.toString()
    }

    fun resolveSourceLabel(uri: Uri): String? = when (uri.authority) {
        "com.android.providers.downloads.documents",
        "com.android.providers.downloads",
        -> SOURCE_DOWNLOADS
        else -> null
    }

    fun resolveAnalyticsSource(uri: Uri): String = when (uri.authority) {
        "com.android.providers.downloads.documents",
        "com.android.providers.downloads",
        -> "downloads"
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        -> "telegram"
        else -> "other"
    }

    const val SOURCE_DOWNLOADS = "downloads"

    private object ContentResolverScheme {
        const val CONTENT = "content"
    }
}
