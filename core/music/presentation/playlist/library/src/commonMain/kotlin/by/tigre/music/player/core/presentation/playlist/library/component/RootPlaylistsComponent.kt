package by.tigre.music.player.core.presentation.playlist.library.component

import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.presentation.appChildStack
import by.tigre.media.platform.presentation.trackScreens
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.playlist.Playlist
import by.tigre.music.player.core.presentation.playlist.library.di.PlaylistsComponentProvider
import by.tigre.music.player.core.presentation.playlist.library.di.PlaylistsDependency
import by.tigre.music.player.core.presentation.playlist.library.navigation.PlaylistsNavigator
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

interface RootPlaylistsComponent {

    val childStack: Value<ChildStack<*, PlaylistsChild>>

    sealed interface PlaylistsChild {
        class PlaylistsList(val component: PlaylistsListComponent) : PlaylistsChild
        class PlaylistDetail(val component: PlaylistDetailComponent) : PlaylistsChild
    }

    @OptIn(DelicateDecomposeApi::class)
    class Impl(
        context: BaseComponentContext,
        dependency: PlaylistsDependency,
        private val componentProvider: PlaylistsComponentProvider,
        private val externalNavigator: PlaylistsNavigator,
    ) : RootPlaylistsComponent, BaseComponentContext by context {

        private val screenAnalytics = dependency.screenAnalytics

        private val navigation = StackNavigation<PlaylistsConfig>()

        private val navigator = object : PlaylistsNavigator {
            override fun openDetail(id: Playlist.Id) {
                navigation.push(PlaylistsConfig.Detail(id.value))
            }

            override fun showPreviousScreen() {
                navigation.pop()
            }

            override fun openArtist(id: Artist.Id) {
                externalNavigator.openArtist(id)
            }

            override fun openAlbum(
                artistId: Artist.Id,
                albumId: Album.Id
            ) {
                externalNavigator.openAlbum(artistId, albumId)
            }
        }

        private val stack = appChildStack(
            source = navigation,
            initialStack = { listOf(PlaylistsConfig.List) },
            childFactory = ::child,
            handleBackButton = true,
        )

        override val childStack: Value<ChildStack<*, PlaylistsChild>> = stack

        init {
            launch {
                stack.trackScreens<PlaylistsConfig, MusicEvents.Screen>(
                    trackScreen = screenAnalytics::trackScreen,
                    name = "PlaylistsConfig",
                ) {
                    when (it) {
                        PlaylistsConfig.List -> MusicEvents.Screen.PlaylistsList
                        is PlaylistsConfig.Detail -> MusicEvents.Screen.PlaylistDetail(it.playlistId)
                    }
                }
            }
        }

        private fun child(
            config: PlaylistsConfig,
            context: BaseComponentContext
        ): PlaylistsChild = when (config) {
            PlaylistsConfig.List -> {
                PlaylistsChild.PlaylistsList(
                    componentProvider.createPlaylistsListComponent(
                        context = context,
                        navigator = navigator,
                    )
                )
            }

            is PlaylistsConfig.Detail -> {
                PlaylistsChild.PlaylistDetail(
                    componentProvider.createPlaylistDetailComponent(
                        context = context,
                        navigator = navigator,
                        playlistId = Playlist.Id(config.playlistId),
                    )
                )
            }
        }

        @Serializable
        private sealed interface PlaylistsConfig {
            @Serializable
            data object List : PlaylistsConfig

            @Serializable
            data class Detail(val playlistId: Long) : PlaylistsConfig
        }
    }
}
