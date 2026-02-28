package by.tigre.audiobook.core.presentation.audiobook_catalog.di

import by.tigre.audiobook.core.presentation.audiobook_catalog.component.BookListComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.FolderSelectionComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.RootAudiobookCatalogComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext

interface AudiobookCatalogComponentProvider {

    fun createRootAudiobookCatalogComponent(context: BaseComponentContext): RootAudiobookCatalogComponent

    fun createFolderSelectionComponent(
        context: BaseComponentContext,
        navigator: AudiobookCatalogNavigator
    ): FolderSelectionComponent

    fun createBookListComponent(
        context: BaseComponentContext,
        navigator: AudiobookCatalogNavigator
    ): BookListComponent

    class Impl(
        private val dependency: AudiobookCatalogDependency
    ) : AudiobookCatalogComponentProvider {

        override fun createRootAudiobookCatalogComponent(
            context: BaseComponentContext
        ): RootAudiobookCatalogComponent = RootAudiobookCatalogComponent.Impl(context, this)

        override fun createFolderSelectionComponent(
            context: BaseComponentContext,
            navigator: AudiobookCatalogNavigator
        ): FolderSelectionComponent = FolderSelectionComponent.Impl(context, dependency, navigator)

        override fun createBookListComponent(
            context: BaseComponentContext,
            navigator: AudiobookCatalogNavigator
        ): BookListComponent = BookListComponent.Impl(context, dependency, navigator)
    }
}
