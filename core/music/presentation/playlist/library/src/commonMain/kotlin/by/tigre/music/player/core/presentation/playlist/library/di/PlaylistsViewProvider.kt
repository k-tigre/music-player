package by.tigre.music.player.core.presentation.playlist.library.di

import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.music.player.core.presentation.playlist.library.component.PlaylistDetailComponent
import by.tigre.music.player.core.presentation.playlist.library.component.PlaylistsListComponent
import by.tigre.music.player.core.presentation.playlist.library.component.RootPlaylistsComponent
import by.tigre.music.player.core.presentation.playlist.library.view.PlaylistDetailView
import by.tigre.music.player.core.presentation.playlist.library.view.PlaylistsListView
import by.tigre.music.player.core.presentation.playlist.library.view.RootPlaylistsView

interface PlaylistsViewProvider {
    fun createRootView(component: RootPlaylistsComponent): ComposableView
    fun createPlaylistsListView(component: PlaylistsListComponent): ComposableView
    fun createPlaylistDetailView(component: PlaylistDetailComponent): ComposableView

    class Impl : PlaylistsViewProvider {
        override fun createRootView(component: RootPlaylistsComponent): RootPlaylistsView =
            RootPlaylistsView(component, this)

        override fun createPlaylistsListView(component: PlaylistsListComponent): PlaylistsListView =
            PlaylistsListView(component)

        override fun createPlaylistDetailView(component: PlaylistDetailComponent): PlaylistDetailView =
            PlaylistDetailView(component)
    }
}
