package by.tigre.audiobook.core.presentation.audiobook_catalog.component

import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.tools.analytics.book.AudiobookEvents
import by.tigre.media.platform.tools.analytics.book.BookEventAnalytics

interface SettingsHubComponent {
    fun onBack()
    fun onThemeClick()
    fun onFoldersClick()
    fun onAboutClick()

    class Impl(
        context: BaseComponentContext,
        private val navigator: AudiobookCatalogNavigator,
        private val eventAnalytics: BookEventAnalytics,
    ) : SettingsHubComponent, BaseComponentContext by context {

        override fun onBack() = navigator.showPreviousScreen()

        override fun onThemeClick() = navigator.showThemeSettings()

        override fun onFoldersClick() {
            eventAnalytics.trackEvent(AudiobookEvents.Action.CatalogOpenFolderSettings)
            navigator.showFolderSelection()
        }

        override fun onAboutClick() = navigator.showAbout()
    }
}
