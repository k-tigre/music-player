package by.tigre.audiobook.core.presentation.audiobook_catalog.component

import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogDependency
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.media.platform.presentation.BaseComponentContext
import kotlinx.coroutines.flow.StateFlow

interface AboutComponent {
    val appVersionName: String
    val showRateApp: StateFlow<Boolean>
    val tipsCount: StateFlow<Int>

    fun onBack()
    fun onScreenShown()
    fun onRateAppClick()
    fun onVersionClick()

    class Impl(
        context: BaseComponentContext,
        private val dependency: AudiobookCatalogDependency,
        private val navigator: AudiobookCatalogNavigator,
    ) : AboutComponent, BaseComponentContext by context {

        override val appVersionName: String = dependency.appVersionName
        override val showRateApp: StateFlow<Boolean> = dependency.showRateApp
        override val tipsCount: StateFlow<Int> = dependency.tipsCount

        private var versionTapCount: Int = 0
        private var lastVersionTapAtMs: Long = 0L

        override fun onBack() = navigator.showPreviousScreen()

        override fun onScreenShown() = dependency.refreshRateAppFlag()

        override fun onRateAppClick() = dependency.onRateAppClick()

        override fun onVersionClick() {
            val now = System.currentTimeMillis()
            if (now - lastVersionTapAtMs > VERSION_TAP_WINDOW_MS) {
                versionTapCount = 0
            }
            lastVersionTapAtMs = now
            versionTapCount += 1
            if (versionTapCount >= VERSION_TAP_TARGET) {
                versionTapCount = 0
                dependency.copyInstallationIdToClipboard()
            }
        }

        private companion object {
            const val VERSION_TAP_TARGET = 7
            const val VERSION_TAP_WINDOW_MS = 2_000L
        }
    }
}
