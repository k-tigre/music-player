package by.tigre.audiobook.core.data.audiobook_playback.prefs

import by.tigre.media.platform.playback.PlaybackSpeed
import by.tigre.media.platform.preferences.Preferences

internal class AudiobookPlaybackSpeedPreferences(
    private val preferences: Preferences,
) {
    fun load(): Float =
        preferences.loadString(KEY, null)
            ?.toFloatOrNull()
            ?.let(PlaybackSpeed::coerce)
            ?: PlaybackSpeed.DEFAULT

    fun save(speed: Float) {
        preferences.saveString(KEY, PlaybackSpeed.coerce(speed).toString())
    }

    companion object {
        private const val KEY = "audiobook_playback_speed"
    }
}
