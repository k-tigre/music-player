package by.tigre.media.platform.background.widget

import android.content.Context
import android.net.Uri

internal data class CachedPlaybackWidgetState(
    val title: String,
    val subtitle: String,
    val artworkUri: Uri?,
    val isPlaying: Boolean,
    val hasActiveMedia: Boolean,
)

internal object PlaybackWidgetStateStore {
    private const val PREFS_NAME = "playback_widget_state"

    private const val KEY_TITLE = "title"
    private const val KEY_SUBTITLE = "subtitle"
    private const val KEY_ARTWORK_URI = "artwork_uri"
    private const val KEY_IS_PLAYING = "is_playing"
    private const val KEY_HAS_ACTIVE_MEDIA = "has_active_media"

    fun save(context: Context, state: CachedPlaybackWidgetState) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TITLE, state.title)
            .putString(KEY_SUBTITLE, state.subtitle)
            .putString(KEY_ARTWORK_URI, state.artworkUri?.toString())
            .putBoolean(KEY_IS_PLAYING, state.isPlaying)
            .putBoolean(KEY_HAS_ACTIVE_MEDIA, state.hasActiveMedia)
            .apply()
    }

    fun load(context: Context): CachedPlaybackWidgetState? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_TITLE)) return null
        val artwork = prefs.getString(KEY_ARTWORK_URI, null)?.takeIf { it.isNotBlank() }?.let(Uri::parse)
        return CachedPlaybackWidgetState(
            title = prefs.getString(KEY_TITLE, "").orEmpty(),
            subtitle = prefs.getString(KEY_SUBTITLE, "").orEmpty(),
            artworkUri = artwork,
            isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false),
            hasActiveMedia = prefs.getBoolean(KEY_HAS_ACTIVE_MEDIA, false),
        )
    }
}
