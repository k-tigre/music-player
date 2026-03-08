package by.tigre.audiobook.core.presentation.audiobook_catalog.di

import by.tigre.audiobook.core.presentation.audiobook_catalog.component.BookListComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.FolderSelectionComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.RootAudiobookCatalogComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.OnBookSelectedListener
import by.tigre.music.player.presentation.base.BaseComponentContext

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

    class Impl(
        private val dependency: AudiobookCatalogDependency
    ) : AudiobookCatalogComponentProvider {

        override fun createRootAudiobookCatalogComponent(
            context: BaseComponentContext,
            onBookSelectedListener: OnBookSelectedListener
        ): RootAudiobookCatalogComponent = RootAudiobookCatalogComponent.Impl(context, this, onBookSelectedListener)

        override fun createFolderSelectionComponent(
            context: BaseComponentContext,
            navigator: AudiobookCatalogNavigator
        ): FolderSelectionComponent = FolderSelectionComponent.Impl(context, dependency, navigator)

        override fun createBookListComponent(
            context: BaseComponentContext,
            navigator: AudiobookCatalogNavigator,
            onBookSelectedListener: OnBookSelectedListener
        ): BookListComponent = BookListComponent.Impl(context, dependency, navigator, onBookSelectedListener)
    }
}
