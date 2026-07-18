package by.tigre.audiobook.core.presentation.audiobook_catalog.di

import by.tigre.audiobook.core.presentation.audiobook_catalog.component.AboutComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.BookListComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.FolderSelectionComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.RootAudiobookCatalogComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.SettingsHubComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.ThemeSettingsComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.AboutView
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.BookListView
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.FolderSelectionView
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.RootAudiobookCatalogView
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.SettingsHubView
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.ThemeSettingsView
import by.tigre.media.platform.tools.platform.compose.ComposableView

class AndroidAudiobookCatalogViewProvider : AudiobookCatalogViewProvider {
    override fun createRootView(component: RootAudiobookCatalogComponent): ComposableView =
        RootAudiobookCatalogView(component, this)

    override fun createFolderSelectionView(component: FolderSelectionComponent): ComposableView =
        FolderSelectionView(component)

    override fun createBookListView(component: BookListComponent): ComposableView =
        BookListView(component)

    override fun createSettingsHubView(component: SettingsHubComponent): ComposableView =
        SettingsHubView(component)

    override fun createThemeSettingsView(component: ThemeSettingsComponent): ComposableView =
        ThemeSettingsView(component)

    override fun createAboutView(component: AboutComponent): ComposableView =
        AboutView(component)
}
