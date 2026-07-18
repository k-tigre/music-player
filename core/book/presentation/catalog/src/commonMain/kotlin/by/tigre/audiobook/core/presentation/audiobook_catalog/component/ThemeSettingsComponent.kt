package by.tigre.audiobook.core.presentation.audiobook_catalog.component

import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogDependency
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.CatalogThemeSettings
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.tools.platform.compose.ContrastPreference
import by.tigre.media.platform.tools.platform.compose.ThemeMode
import kotlinx.coroutines.flow.StateFlow

interface ThemeSettingsComponent {
    val themeSettings: StateFlow<CatalogThemeSettings>

    fun onBack()
    fun setThemeMode(mode: ThemeMode)
    fun setDynamicColor(enabled: Boolean)
    fun setContrast(contrast: ContrastPreference)

    class Impl(
        context: BaseComponentContext,
        private val dependency: AudiobookCatalogDependency,
        private val navigator: AudiobookCatalogNavigator,
    ) : ThemeSettingsComponent, BaseComponentContext by context {

        override val themeSettings: StateFlow<CatalogThemeSettings> = dependency.themeSettings

        override fun onBack() = navigator.showPreviousScreen()

        override fun setThemeMode(mode: ThemeMode) = dependency.setThemeMode(mode)

        override fun setDynamicColor(enabled: Boolean) = dependency.setDynamicColor(enabled)

        override fun setContrast(contrast: ContrastPreference) = dependency.setContrast(contrast)
    }
}
