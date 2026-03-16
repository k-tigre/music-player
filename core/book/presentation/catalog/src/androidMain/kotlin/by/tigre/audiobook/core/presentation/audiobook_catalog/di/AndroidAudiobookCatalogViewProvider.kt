package by.tigre.audiobook.core.presentation.audiobook_catalog.di

import by.tigre.audiobook.core.presentation.audiobook_catalog.component.BookListComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.FolderSelectionComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.RootAudiobookCatalogComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.BookListView
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.FolderSelectionView
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.RootAudiobookCatalogView
import by.tigre.music.player.tools.platform.compose.ComposableView

class AndroidAudiobookCatalogViewProvider : AudiobookCatalogViewProvider {
    override fun createRootView(component: RootAudiobookCatalogComponent): ComposableView =
        RootAudiobookCatalogView(component, this)

    override fun createFolderSelectionView(component: FolderSelectionComponent): ComposableView =
        FolderSelectionView(component)

    override fun createBookListView(component: BookListComponent): ComposableView =
        BookListView(component)
}
