package by.tigre.audiobook.core.presentation.audiobook_catalog.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.RootAudiobookCatalogComponent
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.RootAudiobookCatalogComponent.AudiobookCatalogChild
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogViewProvider
import by.tigre.music.player.tools.platform.compose.ComposableView
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState

class RootAudiobookCatalogView(
    private val component: RootAudiobookCatalogComponent,
    private val viewProvider: AudiobookCatalogViewProvider
) : ComposableView {

    @Composable
    override fun Draw(modifier: Modifier) {
        val childStack by component.childStack.subscribeAsState()

        Children(
            stack = childStack,
            modifier = modifier.fillMaxSize(),
            animation = stackAnimation(fade())
        ) {
            when (val child = it.instance) {
                is AudiobookCatalogChild.FolderSelection ->
                    viewProvider.createFolderSelectionView(child.component).Draw(Modifier)

                is AudiobookCatalogChild.BookList ->
                    viewProvider.createBookListView(child.component).Draw(Modifier)
            }
        }
    }
}
