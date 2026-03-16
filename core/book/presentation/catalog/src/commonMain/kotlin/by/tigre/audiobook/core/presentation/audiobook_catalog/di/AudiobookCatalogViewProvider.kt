package by.tigre.audiobook.core.presentation.audiobook_catalog.di

import by.tigre.audiobook.core.presentation.audiobook_catalog.component.BookListComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.FolderSelectionComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.RootAudiobookCatalogComponent
import by.tigre.music.player.tools.platform.compose.ComposableView

interface AudiobookCatalogViewProvider {

    fun createRootView(component: RootAudiobookCatalogComponent): ComposableView
    fun createFolderSelectionView(component: FolderSelectionComponent): ComposableView
    fun createBookListView(component: BookListComponent): ComposableView
}
