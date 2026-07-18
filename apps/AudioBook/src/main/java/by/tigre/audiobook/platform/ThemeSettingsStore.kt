package by.tigre.audiobook.platform

import by.tigre.media.platform.preferences.ThemePreferencesStorage
import by.tigre.media.platform.tools.platform.compose.ContrastPreference
import by.tigre.media.platform.tools.platform.compose.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val contrast: ContrastPreference = ContrastPreference.Default,
)

class ThemeSettingsStore(
    private val storage: ThemePreferencesStorage,
) {
    private val _state = MutableStateFlow(load())
    val state: StateFlow<ThemeSettings> = _state.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = update { it.copy(mode = mode) }

    fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }

    fun setContrast(contrast: ContrastPreference) = update { it.copy(contrast = contrast) }

    private fun update(transform: (ThemeSettings) -> ThemeSettings) {
        val updated = transform(_state.value)
        save(updated)
        _state.value = updated
    }

    private fun load(): ThemeSettings = ThemeSettings(
        mode = storage.loadThemeMode().toThemeMode(),
        dynamicColor = storage.loadDynamicColor(default = true),
        contrast = storage.loadContrast().toContrastPreference(),
    )

    private fun save(settings: ThemeSettings) {
        storage.saveThemeMode(settings.mode.toStorageValue())
        storage.saveDynamicColor(settings.dynamicColor)
        storage.saveContrast(settings.contrast.toStorageValue())
    }

    private fun String.toThemeMode(): ThemeMode = when (this) {
        ThemePreferencesStorage.THEME_MODE_LIGHT -> ThemeMode.Light
        ThemePreferencesStorage.THEME_MODE_DARK -> ThemeMode.Dark
        else -> ThemeMode.System
    }

    private fun ThemeMode.toStorageValue(): String = when (this) {
        ThemeMode.System -> ThemePreferencesStorage.THEME_MODE_SYSTEM
        ThemeMode.Light -> ThemePreferencesStorage.THEME_MODE_LIGHT
        ThemeMode.Dark -> ThemePreferencesStorage.THEME_MODE_DARK
    }

    private fun String.toContrastPreference(): ContrastPreference = when (this) {
        ThemePreferencesStorage.CONTRAST_MEDIUM -> ContrastPreference.Medium
        ThemePreferencesStorage.CONTRAST_HIGH -> ContrastPreference.High
        else -> ContrastPreference.Default
    }

    private fun ContrastPreference.toStorageValue(): String = when (this) {
        ContrastPreference.Default -> ThemePreferencesStorage.CONTRAST_DEFAULT
        ContrastPreference.Medium -> ThemePreferencesStorage.CONTRAST_MEDIUM
        ContrastPreference.High -> ThemePreferencesStorage.CONTRAST_HIGH
    }
}
