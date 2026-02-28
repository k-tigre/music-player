package by.tigre.audiobook.presentation.root.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogViewProvider
import by.tigre.audiobook.presentation.root.component.Root
import by.tigre.music.player.core.presentation.catalog.di.PlayerViewProvider
import by.tigre.music.player.tools.platform.compose.ComposableView
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.stackAnimation

class RootView(
    private val component: Root,
    private val playerViewProvider: PlayerViewProvider,
    private val audiobookCatalogViewProvider: AudiobookCatalogViewProvider
) : ComposableView {

    @Composable
    override fun Draw(modifier: Modifier) {
        DrawMain()
    }

    @Composable
    private fun DrawMain() {
        Children(
            stack = component.mainComponent,
            animation = stackAnimation(animator = fade())
        ) {
            when (val child = it.instance) {
                is Root.MainComponentChild.Main -> DrawPages()
                is Root.MainComponentChild.Player -> playerViewProvider.createPlayerView(child.component).Draw(Modifier.fillMaxSize())
            }
        }
    }

    @Composable
    private fun DrawPages() {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                playerViewProvider.createSmallPlayerView(component.playerComponent).Draw(Modifier)
            }
        ) { paddings ->
            audiobookCatalogViewProvider.createRootView(component.audiobookCatalogComponent)
                .Draw(Modifier.fillMaxSize().padding(paddings))
        }
    }
}
