package by.tigre.music.player.presentation.settings.component

import by.tigre.media.platform.tools.platform.compose.ContrastPreference
import by.tigre.media.platform.tools.platform.compose.ThemeMode
import by.tigre.music.player.platform.ThemeSettings
import by.tigre.music.player.platform.ThemeSettingsStore
import kotlinx.coroutines.flow.StateFlow

interface SettingsComponent {
    val themeSettings: StateFlow<ThemeSettings>
    val tipsCount: StateFlow<Int>
    val appVersionName: String
    fun setThemeMode(mode: ThemeMode)
    fun setDynamicColor(enabled: Boolean)
    fun setContrast(contrast: ContrastPreference)
    fun upgrade()
    fun restorePurchases()
    fun tips()
    fun close()
    fun onVersionClick()

    class Impl(
        private val themeSettingsStore: ThemeSettingsStore,
        override val tipsCount: StateFlow<Int>,
        override val appVersionName: String,
        private val onUpgrade: () -> Unit,
        private val onRestorePurchases: () -> Unit,
        private val onTips: () -> Unit,
        private val onClose: () -> Unit,
        private val onCopyInstallationId: () -> Unit,
    ) : SettingsComponent {
        override val themeSettings: StateFlow<ThemeSettings> = themeSettingsStore.state

        private var versionTapCount: Int = 0
        private var lastVersionTapAtMs: Long = 0L

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

        override fun onVersionClick() {
            val now = System.currentTimeMillis()
            if (now - lastVersionTapAtMs > VERSION_TAP_WINDOW_MS) {
                versionTapCount = 0
            }
            lastVersionTapAtMs = now
            versionTapCount += 1
            if (versionTapCount >= VERSION_TAP_TARGET) {
                versionTapCount = 0
                onCopyInstallationId()
            }
        }

        private companion object {
            const val VERSION_TAP_TARGET = 7
            const val VERSION_TAP_WINDOW_MS = 2_000L
        }
    }
}
