package by.tigre.music.player.presentation.settings.component

import by.tigre.media.platform.tools.platform.compose.ContrastPreference
import by.tigre.media.platform.tools.platform.compose.ThemeMode
import by.tigre.music.player.platform.ThemeSettings
import by.tigre.music.player.platform.ThemeSettingsStore
import kotlinx.coroutines.flow.StateFlow

interface SettingsComponent {
    val themeSettings: StateFlow<ThemeSettings>
    val tipsCount: StateFlow<Int>
    fun setThemeMode(mode: ThemeMode)
    fun setDynamicColor(enabled: Boolean)
    fun setContrast(contrast: ContrastPreference)
    fun upgrade()
    fun restorePurchases()
    fun tips()
    fun close()

    class Impl(
        private val themeSettingsStore: ThemeSettingsStore,
        override val tipsCount: StateFlow<Int>,
        private val onUpgrade: () -> Unit,
        private val onRestorePurchases: () -> Unit,
        private val onTips: () -> Unit,
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

        override fun upgrade() = onUpgrade()

        override fun restorePurchases() = onRestorePurchases()

        override fun tips() = onTips()

        override fun close() = onClose()
    }
}
