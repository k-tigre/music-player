package by.tigre.media.platform.playback.prefs

import by.tigre.media.platform.preferences.Preferences

internal data class StoredCustomEqPreset(
    val title: String,
    val gainsDb: List<Float>,
)

internal class EqualizerPreferences(
    private val preferences: Preferences,
) {
    fun loadSelectedPresetIndex(default: Int): Int =
        preferences.loadInt(KEY_SELECTED_PRESET, default)

    fun saveSelectedPresetIndex(index: Int) {
        preferences.saveInt(KEY_SELECTED_PRESET, index)
    }

    fun loadSuggestSetup(default: Boolean = true): Boolean =
        preferences.loadBoolean(KEY_SUGGEST_SETUP, default)

    fun saveSuggestSetup(enabled: Boolean) {
        preferences.saveBoolean(KEY_SUGGEST_SETUP, enabled)
    }

    /**
     * Always returns 1..[MAX_CUSTOM_PRESETS] slots.
     * Migrates legacy single-gains key when multi-slot storage is absent.
     */
    fun loadCustomPresets(defaultTitle: String = DEFAULT_CUSTOM_TITLE): List<StoredCustomEqPreset> {
        val count = preferences.loadInt(KEY_CUSTOM_COUNT, -1)
        if (count < 0) {
            val legacy = loadLegacyCustomBandGainsDb()
            val migrated = listOf(
                StoredCustomEqPreset(
                    title = defaultTitle,
                    gainsDb = legacy.orEmpty(),
                ),
            )
            saveCustomPresets(migrated)
            return migrated
        }
        val n = count.coerceIn(1, MAX_CUSTOM_PRESETS)
        return List(n) { i ->
            val title = preferences.loadString(keyTitle(i), null)
                ?.takeIf { it.isNotBlank() }
                ?: defaultCustomTitle(i, defaultTitle)
            val gains = parseGains(preferences.loadString(keyGains(i), null))
            StoredCustomEqPreset(title = title, gainsDb = gains)
        }
    }

    fun saveCustomPresets(presets: List<StoredCustomEqPreset>) {
        val clipped = presets.take(MAX_CUSTOM_PRESETS).ifEmpty {
            listOf(StoredCustomEqPreset(DEFAULT_CUSTOM_TITLE, emptyList()))
        }
        preferences.saveInt(KEY_CUSTOM_COUNT, clipped.size)
        clipped.forEachIndexed { i, preset ->
            preferences.saveString(keyTitle(i), preset.title)
            preferences.saveString(keyGains(i), preset.gainsDb.joinToString(",") { it.toString() })
        }
        // Clear leftover slots from a previously larger list.
        for (i in clipped.size until MAX_CUSTOM_PRESETS) {
            preferences.saveString(keyTitle(i), "")
            preferences.saveString(keyGains(i), "")
        }
    }

    /** Legacy single-slot API kept for migration only. */
    private fun loadLegacyCustomBandGainsDb(): List<Float>? {
        val s = preferences.loadString(KEY_CUSTOM_GAINS, null) ?: return null
        if (s.isBlank()) return null
        return parseGains(s).takeIf { it.isNotEmpty() }
    }

    companion object {
        const val MAX_CUSTOM_PRESETS = 3
        const val DEFAULT_CUSTOM_TITLE = "Custom"

        private const val KEY_SELECTED_PRESET = "playback_equalizer_selected_preset_index"
        private const val KEY_CUSTOM_GAINS = "playback_equalizer_custom_band_gains_db"
        private const val KEY_SUGGEST_SETUP = "playback_equalizer_suggest_setup"
        private const val KEY_CUSTOM_COUNT = "playback_equalizer_custom_count"
        private fun keyTitle(i: Int) = "playback_equalizer_custom_title_$i"
        private fun keyGains(i: Int) = "playback_equalizer_custom_gains_$i"

        fun defaultCustomTitle(slot: Int, base: String = DEFAULT_CUSTOM_TITLE): String =
            if (slot == 0) base else "$base ${slot + 1}"

        private fun parseGains(raw: String?): List<Float> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.split(',').mapNotNull { it.trim().toFloatOrNull() }
        }
    }
}

internal fun alignGainsToBandCount(saved: List<Float>?, bandCount: Int): List<Float> =
    List(bandCount) { i -> saved?.getOrNull(i) ?: 0f }
