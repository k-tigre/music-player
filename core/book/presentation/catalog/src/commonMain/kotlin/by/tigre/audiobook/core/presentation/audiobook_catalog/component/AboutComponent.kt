package by.tigre.audiobook.core.presentation.audiobook_catalog.component

import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogDependency
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.media.platform.presentation.BaseComponentContext
import kotlinx.coroutines.flow.StateFlow

interface AboutComponent {
    val appVersionName: String
    val showRateApp: StateFlow<Boolean>

    fun onBack()
    fun onScreenShown()
    fun onRateAppClick()

    class Impl(
        context: BaseComponentContext,
        private val dependency: AudiobookCatalogDependency,
        private val navigator: AudiobookCatalogNavigator,
    ) : AboutComponent, BaseComponentContext by context {

        override val appVersionName: String = dependency.appVersionName
        override val showRateApp: StateFlow<Boolean> = dependency.showRateApp

        override fun onBack() = navigator.showPreviousScreen()

        override fun onScreenShown() = dependency.refreshRateAppFlag()

        override fun onRateAppClick() = dependency.onRateAppClick()
    }
}
