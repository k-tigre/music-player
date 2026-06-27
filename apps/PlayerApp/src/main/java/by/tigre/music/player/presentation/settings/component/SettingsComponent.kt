package by.tigre.music.player.presentation.settings.component

import by.tigre.media.platform.tools.platform.compose.ContrastPreference
import by.tigre.media.platform.tools.platform.compose.ThemeMode
import by.tigre.music.player.platform.ThemeSettings
import by.tigre.music.player.platform.ThemeSettingsStore
import kotlinx.coroutines.flow.StateFlow

interface SettingsComponent {
    val themeSettings: StateFlow<ThemeSettings>
    fun setThemeMode(mode: ThemeMode)
    fun setDynamicColor(enabled: Boolean)
    fun setContrast(contrast: ContrastPreference)
    fun close()

    class Impl(
        private val themeSettingsStore: ThemeSettingsStore,
        private val onClose: () -> Unit,
    ) : SettingsComponent {
        override val themeSettings: StateFlow<ThemeSettings> = themeSettingsStore.state

        override fun setThemeMode(mode: ThemeMode) {
            themeSettingsStore.setThemeMode(mode)
        }

        override fun setDynamicColor(enabled: Boolean) {
            themeSettingsStore.setDynamicColor(enabled)
        }

        override fun setContrast(contrast: ContrastPreference) {
            themeSettingsStore.setContrast(contrast)
        }

        override fun close() = onClose()
    }
}
