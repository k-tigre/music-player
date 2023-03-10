package by.tigre.music.player.core.presentation.catalog.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import by.tigre.music.player.core.presentation.catalog.component.RootCatalogComponent
import by.tigre.music.player.core.presentation.catalog.component.RootCatalogComponent.CatalogChild
import by.tigre.music.player.core.presentation.catalog.di.CatalogViewProvider
import by.tigre.music.player.tools.platform.compose.ComposableView
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState

@OptIn(ExperimentalDecomposeApi::class)
class RootCatalogView(
    private val component: RootCatalogComponent,
    private val viewProvider: CatalogViewProvider
) : ComposableView {

    @Composable
    override fun Draw() {
        val childStack by component.childStack.subscribeAsState()

        Children(
            stack = childStack,
            modifier = Modifier.fillMaxSize(),
            animation = stackAnimation(fade())
        ) {
            when (val child = it.instance) {
                is CatalogChild.AlbumsList -> {
                    viewProvider.createAlbumsListView(child.component).Draw()
                }

                is CatalogChild.ArtistsList -> {
                    viewProvider.createArtistsListView(child.component).Draw()
                }

                is CatalogChild.SongsList -> {
                    viewProvider.createSongsListView(child.component).Draw()
                }

            }
        }
    }
}
