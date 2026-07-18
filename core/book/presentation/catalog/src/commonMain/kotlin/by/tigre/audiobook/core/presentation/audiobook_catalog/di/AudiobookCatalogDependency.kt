package by.tigre.audiobook.core.presentation.audiobook_catalog.di

import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.presentation.audiobook_catalog.scan.CatalogScanCoordinator
import by.tigre.media.platform.tools.analytics.book.BookAnalyticsDependency
import by.tigre.media.platform.tools.platform.compose.ContrastPreference
import by.tigre.media.platform.tools.platform.compose.ThemeMode
import kotlinx.coroutines.flow.StateFlow

data class CatalogThemeSettings(
    val mode: ThemeMode,
    val dynamicColor: Boolean,
    val contrast: ContrastPreference,
)

interface AudiobookCatalogDependency : BookAnalyticsDependency {
    val audiobookCatalogSource: AudiobookCatalogSource
    val audiobookPlaybackController: AudiobookPlaybackController
    val catalogScanCoordinator: CatalogScanCoordinator

    val themeSettings: StateFlow<CatalogThemeSettings>
    fun setThemeMode(mode: ThemeMode)
    fun setDynamicColor(enabled: Boolean)
    fun setContrast(contrast: ContrastPreference)

    val appVersionName: String
    val showRateApp: StateFlow<Boolean>
    fun refreshRateAppFlag()
    fun onRateAppClick()
}
