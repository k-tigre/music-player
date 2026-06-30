package by.tigre.media.platform.playback.prefs

import by.tigre.media.platform.preferences.Preferences

internal class EqualizerPreferences(
    private val preferences: Preferences,
) {
    data class SavedState(
        val selectedPresetIndex: Int,
        val customBandGainsDb: List<Float>?,
    )

    fun loadState(defaultPresetIndex: Int = 0): SavedState =
        SavedState(
            selectedPresetIndex = loadSelectedPresetIndex(defaultPresetIndex),
            customBandGainsDb = loadCustomBandGainsDb(),
        )

    fun loadSelectedPresetIndex(default: Int): Int =
        preferences.loadInt(KEY_SELECTED_PRESET, default)

    fun saveSelectedPresetIndex(index: Int) {
        saveState(selectedPresetIndex = index, customBandGainsDb = null)
    }

    fun loadCustomBandGainsDb(): List<Float>? {
        val s = preferences.loadString(KEY_CUSTOM_GAINS, null) ?: return null
        if (s.isBlank()) return null
        val list = s.split(',').mapNotNull { it.trim().toFloatOrNull() }
        return list.takeIf { it.isNotEmpty() }
    }

    fun saveCustomBandGainsDb(gains: List<Float>) {
        saveState(selectedPresetIndex = loadSelectedPresetIndex(0), customBandGainsDb = gains)
    }

    fun saveState(selectedPresetIndex: Int, customBandGainsDb: List<Float>?) {
        val gainsString = when {
            customBandGainsDb != null -> customBandGainsDb.joinToString(",") { it.toString() }
            else -> preferences.loadString(KEY_CUSTOM_GAINS, null).orEmpty()
        }
        preferences.save {
            put(KEY_SELECTED_PRESET, selectedPresetIndex)
            put(KEY_CUSTOM_GAINS, gainsString)
        }
    }

    companion object {
        private const val KEY_SELECTED_PRESET = "playback_equalizer_selected_preset_index"
        private const val KEY_CUSTOM_GAINS = "playback_equalizer_custom_band_gains_db"
    }
}

internal fun alignGainsToBandCount(saved: List<Float>?, bandCount: Int): List<Float> =
    List(bandCount) { i -> saved?.getOrNull(i) ?: 0f }
