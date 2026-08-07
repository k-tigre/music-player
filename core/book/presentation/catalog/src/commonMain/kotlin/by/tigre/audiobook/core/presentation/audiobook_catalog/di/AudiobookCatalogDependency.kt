package by.tigre.audiobook.core.presentation.audiobook_catalog.di

import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook.spaces.LibrarySpaceRepository
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.presentation.audiobook_catalog.scan.CatalogScanCoordinator
import by.tigre.media.platform.entitlements.EntitlementsRepository
import by.tigre.media.platform.entitlements.Feature
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
    val librarySpaceRepository: LibrarySpaceRepository
    val catalogScanCoordinator: CatalogScanCoordinator
    val entitlementsRepository: EntitlementsRepository

    val themeSettings: StateFlow<CatalogThemeSettings>
    fun setThemeMode(mode: ThemeMode)
    fun setDynamicColor(enabled: Boolean)
    fun setContrast(contrast: ContrastPreference)

    val appVersionName: String
    val showRateApp: StateFlow<Boolean>
    val tipsCount: StateFlow<Int>
    fun refreshRateAppFlag()
    fun onRateAppClick()
    fun requestUpgrade()
    fun requestPaywall(feature: Feature, source: String = feature.name)
    fun restorePurchases()
    fun requestTips()
    fun copyInstallationIdToClipboard()
}
