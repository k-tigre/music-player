package by.tigre.audiobook.core.presentation.audiobook_catalog.component

import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogDependency
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.tools.analytics.book.AudiobookEvents
import by.tigre.media.platform.tools.analytics.book.BookEventAnalytics

interface SettingsHubComponent {
    fun onBack()
    fun onThemeClick()
    fun onFoldersClick()
    fun onAboutClick()
    fun onUpgradeClick()
    fun onRestoreClick()
    fun onTipsClick()

    class Impl(
        context: BaseComponentContext,
        private val navigator: AudiobookCatalogNavigator,
        private val dependency: AudiobookCatalogDependency,
    ) : SettingsHubComponent, BaseComponentContext by context {

        override fun onBack() = navigator.showPreviousScreen()

        override fun onThemeClick() = navigator.showThemeSettings()

        override fun onFoldersClick() {
            dependency.eventAnalytics.trackEvent(AudiobookEvents.Action.CatalogOpenFolderSettings)
            navigator.showFolderSelection()
        }

        override fun onAboutClick() = navigator.showAbout()

        override fun onUpgradeClick() = dependency.requestUpgrade()

        override fun onRestoreClick() = dependency.restorePurchases()

        override fun onTipsClick() = dependency.requestTips()
    }
}
