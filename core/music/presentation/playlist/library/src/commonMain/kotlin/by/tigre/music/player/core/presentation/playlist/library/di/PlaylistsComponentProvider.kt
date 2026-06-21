package by.tigre.music.player.core.presentation.playlist.library.di

import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.music.player.core.entiry.playlist.Playlist
import by.tigre.music.player.core.presentation.playlist.library.component.PlaylistDetailComponent
import by.tigre.music.player.core.presentation.playlist.library.component.PlaylistsListComponent
import by.tigre.music.player.core.presentation.playlist.library.component.RootPlaylistsComponent
import by.tigre.music.player.core.presentation.playlist.library.navigation.PlaylistsNavigator

interface PlaylistsComponentProvider {
    fun createRootPlaylistsComponent(
        context: BaseComponentContext,
        navigator: PlaylistsNavigator
    ): RootPlaylistsComponent

    fun createPlaylistsListComponent(
        context: BaseComponentContext,
        navigator: PlaylistsNavigator
    ): PlaylistsListComponent

    fun createPlaylistDetailComponent(
        context: BaseComponentContext,
        navigator: PlaylistsNavigator,
        playlistId: Playlist.Id
    ): PlaylistDetailComponent

    class Impl(
        private val dependency: PlaylistsDependency
    ) : PlaylistsComponentProvider {
        override fun createRootPlaylistsComponent(
            context: BaseComponentContext,
            navigator: PlaylistsNavigator
        ): RootPlaylistsComponent = RootPlaylistsComponent.Impl(context, dependency, this, navigator)

        override fun createPlaylistsListComponent(
            context: BaseComponentContext,
            navigator: PlaylistsNavigator
        ): PlaylistsListComponent = PlaylistsListComponent.Impl(context, dependency, navigator)

        override fun createPlaylistDetailComponent(
            context: BaseComponentContext,
            navigator: PlaylistsNavigator,
            playlistId: Playlist.Id
        ): PlaylistDetailComponent = PlaylistDetailComponent.Impl(context, dependency, navigator, playlistId)
    }
}
