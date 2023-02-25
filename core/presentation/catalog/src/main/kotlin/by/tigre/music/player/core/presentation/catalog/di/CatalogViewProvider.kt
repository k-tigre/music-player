package by.tigre.music.player.core.presentation.catalog.di

import by.tigre.music.player.core.presentation.catalog.component.AlbumListComponent
import by.tigre.music.player.core.presentation.catalog.component.ArtistListComponent
import by.tigre.music.player.core.presentation.catalog.component.CatalogFolderSelectorComponent
import by.tigre.music.player.core.presentation.catalog.component.RootCatalogComponent
import by.tigre.music.player.core.presentation.catalog.component.SongsListComponent
import by.tigre.music.player.core.presentation.catalog.view.AlbumListView
import by.tigre.music.player.core.presentation.catalog.view.ArtistListView
import by.tigre.music.player.core.presentation.catalog.view.CatalogFolderView
import by.tigre.music.player.core.presentation.catalog.view.RootCatalogView
import by.tigre.music.player.core.presentation.catalog.view.SongsListView
import by.tigre.music.player.tools.platform.compose.ComposableView

interface CatalogViewProvider {
    fun createRootView(component: RootCatalogComponent): ComposableView
    fun createFolderView(component: CatalogFolderSelectorComponent): ComposableView
    fun createAlbumsListView(component: AlbumListComponent): ComposableView
    fun createArtistsListView(component: ArtistListComponent): ComposableView
    fun createSongsListView(component: SongsListComponent): ComposableView

    class Impl(

    ) : CatalogViewProvider {
        override fun createRootView(component: RootCatalogComponent): RootCatalogView = RootCatalogView(component, this)
        override fun createAlbumsListView(component: AlbumListComponent): AlbumListView = AlbumListView(component)
        override fun createArtistsListView(component: ArtistListComponent): ArtistListView = ArtistListView(component)
        override fun createSongsListView(component: SongsListComponent): SongsListView = SongsListView(component)
        override fun createFolderView(component: CatalogFolderSelectorComponent): CatalogFolderView = CatalogFolderView(component)
    }
}
