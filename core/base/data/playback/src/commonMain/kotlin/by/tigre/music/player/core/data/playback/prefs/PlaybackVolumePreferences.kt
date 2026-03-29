package by.tigre.music.player.core.data.playback.prefs

import by.tigre.music.player.core.data.storage.preferences.Preferences

internal class PlaybackVolumePreferences(
    private val preferences: Preferences,
) {
    fun load(): Float =
        preferences.loadString(KEY, null)
            ?.toFloatOrNull()
            ?.coerceIn(0f, 1f)
            ?: 1f

    fun save(volume: Float) {
        preferences.saveString(KEY, volume.coerceIn(0f, 1f).toString())
    }

    companion object {
        private const val KEY = "playback_app_volume_linear"
    }
}
