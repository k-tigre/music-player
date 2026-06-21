package by.tigre.music.player.core.presentation.playlist.library.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.music.player.core.presentation.playlist.library.component.RootPlaylistsComponent
import by.tigre.music.player.core.presentation.playlist.library.component.RootPlaylistsComponent.PlaylistsChild
import by.tigre.music.player.core.presentation.playlist.library.di.PlaylistsViewProvider
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@OptIn(ExperimentalDecomposeApi::class)
class RootPlaylistsView(
    private val component: RootPlaylistsComponent,
    private val viewProvider: PlaylistsViewProvider,
) : ComposableView {

    @Composable
    override fun Draw(modifier: Modifier) {
        val childStack by component.childStack.subscribeAsState()
        Children(
            stack = childStack,
            modifier = modifier.fillMaxSize(),
            animation = stackAnimation(fade()),
        ) {
            when (val child = it.instance) {
                is PlaylistsChild.PlaylistsList ->
                    viewProvider.createPlaylistsListView(child.component).Draw(Modifier.fillMaxSize())

                is PlaylistsChild.PlaylistDetail ->
                    viewProvider.createPlaylistDetailView(child.component).Draw(Modifier.fillMaxSize())
            }
        }
    }
}
