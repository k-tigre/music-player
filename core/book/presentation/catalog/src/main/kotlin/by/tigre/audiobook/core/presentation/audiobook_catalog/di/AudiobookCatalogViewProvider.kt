package by.tigre.audiobook.core.presentation.audiobook_catalog.di

import by.tigre.audiobook.core.presentation.audiobook_catalog.component.BookListComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.FolderSelectionComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.RootAudiobookCatalogComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.BookListView
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.FolderSelectionView
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.RootAudiobookCatalogView
import by.tigre.music.player.tools.platform.compose.ComposableView

interface AudiobookCatalogViewProvider {

    fun createRootView(component: RootAudiobookCatalogComponent): ComposableView
    fun createFolderSelectionView(component: FolderSelectionComponent): ComposableView
    fun createBookListView(component: BookListComponent): ComposableView

    class Impl : AudiobookCatalogViewProvider {
        override fun createRootView(component: RootAudiobookCatalogComponent) =
            RootAudiobookCatalogView(component, this)

        override fun createFolderSelectionView(component: FolderSelectionComponent) =
            FolderSelectionView(component)

        override fun createBookListView(component: BookListComponent) =
            BookListView(component)
    }
}
