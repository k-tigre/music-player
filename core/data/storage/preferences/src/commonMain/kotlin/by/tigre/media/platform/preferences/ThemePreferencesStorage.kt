package by.tigre.media.platform.preferences

class ThemePreferencesStorage(
    private val preferences: Preferences,
) {
    fun loadThemeMode(): String =
        preferences.loadString(KEY_THEME_MODE, DEFAULT_THEME_MODE) ?: DEFAULT_THEME_MODE

    fun saveThemeMode(mode: String) {
        preferences.saveString(KEY_THEME_MODE, mode)
    }

    fun loadDynamicColor(default: Boolean = true): Boolean =
        preferences.loadBoolean(KEY_DYNAMIC_COLOR, default)

    fun saveDynamicColor(enabled: Boolean) {
        preferences.saveBoolean(KEY_DYNAMIC_COLOR, enabled)
    }

    fun loadContrast(): String =
        preferences.loadString(KEY_CONTRAST, DEFAULT_CONTRAST) ?: DEFAULT_CONTRAST

    fun saveContrast(contrast: String) {
        preferences.saveString(KEY_CONTRAST, contrast)
    }

    companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DYNAMIC_COLOR = "theme_dynamic_color"
        const val KEY_CONTRAST = "theme_contrast"

        const val THEME_MODE_SYSTEM = "system"
        const val THEME_MODE_LIGHT = "light"
        const val THEME_MODE_DARK = "dark"

        const val CONTRAST_DEFAULT = "default"
        const val CONTRAST_MEDIUM = "medium"
        const val CONTRAST_HIGH = "high"

        const val DEFAULT_THEME_MODE = THEME_MODE_SYSTEM
        const val DEFAULT_CONTRAST = CONTRAST_DEFAULT
    }
}
