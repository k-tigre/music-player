package by.tigre.music.player.core.presentation.favorites.di

import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.music.player.core.presentation.favorites.component.FavoritesComponent
import by.tigre.music.player.core.presentation.favorites.navigation.FavoritesNavigator
import by.tigre.music.player.core.presentation.favorites.view.FavoritesView

interface FavoritesComponentProvider {
    fun createFavoritesComponent(
        context: BaseComponentContext,
        navigator: FavoritesNavigator,
    ): FavoritesComponent

    class Impl(
        private val dependency: FavoritesDependency,
    ) : FavoritesComponentProvider {
        override fun createFavoritesComponent(
            context: BaseComponentContext,
            navigator: FavoritesNavigator,
        ): FavoritesComponent = FavoritesComponent.Impl(
            context = context,
            dependency = dependency,
            navigator = navigator,
        )
    }
}

interface FavoritesViewProvider {
    fun createFavoritesView(component: FavoritesComponent): FavoritesView

    class Impl(
        private val albumArtProvider: by.tigre.music.player.core.data.catalog.AlbumArtProvider,
        private val artistArtProvider: by.tigre.music.player.core.data.catalog.ArtistArtProvider,
    ) : FavoritesViewProvider {
        override fun createFavoritesView(component: FavoritesComponent): FavoritesView =
            FavoritesView(component, albumArtProvider, artistArtProvider)
    }
}
