package by.tigre.music.player.core.data.playback.prefs

import by.tigre.music.player.core.data.storage.preferences.Preferences

internal class EqualizerPreferences(
    private val preferences: Preferences,
) {
    fun loadSelectedPresetIndex(default: Int): Int =
        preferences.loadInt(KEY_SELECTED_PRESET, default)

    fun saveSelectedPresetIndex(index: Int) {
        preferences.saveInt(KEY_SELECTED_PRESET, index)
    }

    fun loadCustomBandGainsDb(): List<Float>? {
        val s = preferences.loadString(KEY_CUSTOM_GAINS, null) ?: return null
        if (s.isBlank()) return null
        val list = s.split(',').mapNotNull { it.trim().toFloatOrNull() }
        return list.takeIf { it.isNotEmpty() }
    }

    fun saveCustomBandGainsDb(gains: List<Float>) {
        preferences.saveString(KEY_CUSTOM_GAINS, gains.joinToString(",") { it.toString() })
    }

    companion object {
        private const val KEY_SELECTED_PRESET = "playback_equalizer_selected_preset_index"
        private const val KEY_CUSTOM_GAINS = "playback_equalizer_custom_band_gains_db"
    }
}

internal fun alignGainsToBandCount(saved: List<Float>?, bandCount: Int): List<Float> =
    List(bandCount) { i -> saved?.getOrNull(i) ?: 0f }
