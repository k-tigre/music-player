package by.tigre.audiobook.core.presentation.audiobook_catalog.di

import by.tigre.audiobook.core.presentation.audiobook_catalog.component.AboutComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.BookListComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.FolderSelectionComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.RootAudiobookCatalogComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.SettingsHubComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.ThemeSettingsComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.OnBookSelectedListener
import by.tigre.media.platform.presentation.BaseComponentContext

interface AudiobookCatalogComponentProvider {

    fun createRootAudiobookCatalogComponent(
        context: BaseComponentContext,
        onBookSelectedListener: OnBookSelectedListener
    ): RootAudiobookCatalogComponent

    fun createFolderSelectionComponent(
        context: BaseComponentContext,
        navigator: AudiobookCatalogNavigator
    ): FolderSelectionComponent

    fun createBookListComponent(
        context: BaseComponentContext,
        navigator: AudiobookCatalogNavigator,
        onBookSelectedListener: OnBookSelectedListener
    ): BookListComponent

    fun createSettingsHubComponent(
        context: BaseComponentContext,
        navigator: AudiobookCatalogNavigator
    ): SettingsHubComponent

    fun createThemeSettingsComponent(
        context: BaseComponentContext,
        navigator: AudiobookCatalogNavigator
    ): ThemeSettingsComponent

    fun createAboutComponent(
        context: BaseComponentContext,
        navigator: AudiobookCatalogNavigator
    ): AboutComponent

    class Impl(
        private val dependency: AudiobookCatalogDependency
    ) : AudiobookCatalogComponentProvider {

        override fun createRootAudiobookCatalogComponent(
            context: BaseComponentContext,
            onBookSelectedListener: OnBookSelectedListener
        ): RootAudiobookCatalogComponent =
            RootAudiobookCatalogComponent.Impl(context, this, dependency, onBookSelectedListener)

        override fun createFolderSelectionComponent(
            context: BaseComponentContext,
            navigator: AudiobookCatalogNavigator
        ): FolderSelectionComponent = FolderSelectionComponent.Impl(context, dependency, navigator)

        override fun createBookListComponent(
            context: BaseComponentContext,
            navigator: AudiobookCatalogNavigator,
            onBookSelectedListener: OnBookSelectedListener
        ): BookListComponent = BookListComponent.Impl(context, dependency, navigator, onBookSelectedListener)

        override fun createSettingsHubComponent(
            context: BaseComponentContext,
            navigator: AudiobookCatalogNavigator
        ): SettingsHubComponent = SettingsHubComponent.Impl(context, navigator, dependency.eventAnalytics)

        override fun createThemeSettingsComponent(
            context: BaseComponentContext,
            navigator: AudiobookCatalogNavigator
        ): ThemeSettingsComponent = ThemeSettingsComponent.Impl(context, dependency, navigator)

        override fun createAboutComponent(
            context: BaseComponentContext,
            navigator: AudiobookCatalogNavigator
        ): AboutComponent = AboutComponent.Impl(context, dependency, navigator)
    }
}
