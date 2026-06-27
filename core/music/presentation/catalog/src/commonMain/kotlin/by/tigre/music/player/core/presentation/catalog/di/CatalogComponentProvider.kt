package by.tigre.music.player.core.presentation.catalog.di

import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.presentation.catalog.component.AlbumListComponent
import by.tigre.music.player.core.presentation.catalog.component.ArtistListComponent
import by.tigre.music.player.core.presentation.catalog.component.RootCatalogComponent
import by.tigre.music.player.core.presentation.catalog.component.RootCatalogComponent.Impl
import by.tigre.music.player.core.presentation.catalog.component.SongsListComponent
import by.tigre.music.player.core.presentation.catalog.navigation.CatalogNavigator
import by.tigre.media.platform.presentation.BaseComponentContext

interface CatalogComponentProvider {
    fun createRootCatalogComponent(
        context: BaseComponentContext,
        onOpenSettings: (() -> Unit)? = null,
    ): RootCatalogComponent
    fun createArtistListComponent(
        context: BaseComponentContext,
        navigator: CatalogNavigator,
        onOpenSettings: (() -> Unit)? = null,
    ): ArtistListComponent
    fun createAlbumListComponent(
        context: BaseComponentContext,
        navigator: CatalogNavigator,
        artist: Artist
    ): AlbumListComponent

    fun createSongsListComponent(
        context: BaseComponentContext,
        navigator: CatalogNavigator,
        album: Album,
        artist: Artist
    ): SongsListComponent

    class Impl(
        private val dependency: CatalogDependency
    ) : CatalogComponentProvider {
        override fun createRootCatalogComponent(
            context: BaseComponentContext,
            onOpenSettings: (() -> Unit)?,
        ): RootCatalogComponent = Impl(context, this, dependency, onOpenSettings)

        override fun createArtistListComponent(
            context: BaseComponentContext,
            navigator: CatalogNavigator,
            onOpenSettings: (() -> Unit)?,
        ): ArtistListComponent = ArtistListComponent.Impl(context, dependency, navigator, onOpenSettings)

        override fun createAlbumListComponent(
            context: BaseComponentContext,
            navigator: CatalogNavigator,
            artist: Artist
        ): AlbumListComponent = AlbumListComponent.Impl(context, dependency, navigator, artist)

        override fun createSongsListComponent(
            context: BaseComponentContext,
            navigator: CatalogNavigator,
            album: Album,
            artist: Artist
        ): SongsListComponent = SongsListComponent.Impl(context, dependency, navigator, album, artist)
    }


}
