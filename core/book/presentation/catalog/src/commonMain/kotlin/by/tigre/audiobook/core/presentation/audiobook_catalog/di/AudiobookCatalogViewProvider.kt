package by.tigre.audiobook.core.presentation.audiobook_catalog.di

import by.tigre.audiobook.core.presentation.audiobook_catalog.component.AboutComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.BookListComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.FolderSelectionComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.RootAudiobookCatalogComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.SettingsHubComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.ThemeSettingsComponent
import by.tigre.media.platform.tools.platform.compose.ComposableView

interface AudiobookCatalogViewProvider {

    fun createRootView(component: RootAudiobookCatalogComponent): ComposableView
    fun createFolderSelectionView(component: FolderSelectionComponent): ComposableView
    fun createBookListView(component: BookListComponent): ComposableView
    fun createSettingsHubView(component: SettingsHubComponent): ComposableView
    fun createThemeSettingsView(component: ThemeSettingsComponent): ComposableView
    fun createAboutView(component: AboutComponent): ComposableView
}
