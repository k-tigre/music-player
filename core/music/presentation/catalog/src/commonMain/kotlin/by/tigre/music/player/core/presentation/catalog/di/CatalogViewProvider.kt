package by.tigre.music.player.core.presentation.catalog.di

import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.data.catalog.ArtistArtProvider
import by.tigre.music.player.core.presentation.catalog.component.AlbumListComponent
import by.tigre.music.player.core.presentation.catalog.component.ArtistListComponent
import by.tigre.music.player.core.presentation.catalog.component.RootCatalogComponent
import by.tigre.music.player.core.presentation.catalog.component.SongsListComponent
import by.tigre.music.player.core.presentation.catalog.view.AlbumListView
import by.tigre.music.player.core.presentation.catalog.view.ArtistListView
import by.tigre.music.player.core.presentation.catalog.view.RootCatalogView
import by.tigre.music.player.core.presentation.catalog.view.SongsListView
import by.tigre.media.platform.tools.platform.compose.ComposableView

interface CatalogViewProvider {
    fun createRootView(component: RootCatalogComponent): ComposableView
    fun createAlbumsListView(component: AlbumListComponent): ComposableView
    fun createArtistsListView(component: ArtistListComponent): ComposableView
    fun createSongsListView(component: SongsListComponent): ComposableView

    class Impl(
        private val albumArtProvider: AlbumArtProvider,
        private val artistArtProvider: ArtistArtProvider,
    ) : CatalogViewProvider {
        override fun createRootView(component: RootCatalogComponent): RootCatalogView = RootCatalogView(component, this)
        override fun createAlbumsListView(component: AlbumListComponent): AlbumListView =
            AlbumListView(component, albumArtProvider)
        override fun createArtistsListView(component: ArtistListComponent): ArtistListView =
            ArtistListView(component, albumArtProvider, artistArtProvider)
        override fun createSongsListView(component: SongsListComponent): SongsListView =
            SongsListView(component, albumArtProvider)
    }
}
