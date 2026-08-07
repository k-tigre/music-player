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
        navigator: PlaylistsNavigator,
        canCreatePlaylist: suspend () -> Boolean = { true },
    ): RootPlaylistsComponent

    fun createPlaylistsListComponent(
        context: BaseComponentContext,
        navigator: PlaylistsNavigator,
        canCreatePlaylist: suspend () -> Boolean = { true },
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
            navigator: PlaylistsNavigator,
            canCreatePlaylist: suspend () -> Boolean,
        ): RootPlaylistsComponent =
            RootPlaylistsComponent.Impl(context, dependency, this, navigator, canCreatePlaylist)

        override fun createPlaylistsListComponent(
            context: BaseComponentContext,
            navigator: PlaylistsNavigator,
            canCreatePlaylist: suspend () -> Boolean,
        ): PlaylistsListComponent =
            PlaylistsListComponent.Impl(context, dependency, navigator, canCreatePlaylist)

        override fun createPlaylistDetailComponent(
            context: BaseComponentContext,
            navigator: PlaylistsNavigator,
            playlistId: Playlist.Id
        ): PlaylistDetailComponent = PlaylistDetailComponent.Impl(context, dependency, navigator, playlistId)
    }
}
