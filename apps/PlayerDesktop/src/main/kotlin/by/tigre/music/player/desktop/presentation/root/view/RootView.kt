package by.tigre.music.player.desktop.presentation.root.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.presentation.catalog.di.CatalogViewProvider
import by.tigre.music.player.core.presentation.catalog.di.PlayerViewProvider
import by.tigre.music.player.core.presentation.catalog.view.PlayerView
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueViewProvider
import by.tigre.music.player.desktop.presentation.root.component.Root
import by.tigre.music.player.tools.platform.compose.ComposableView
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import javax.swing.JFileChooser

class RootView(
    private val component: Root,
    private val catalogViewProvider: CatalogViewProvider,
    private val playerViewProvider: PlayerViewProvider,
    private val currentQueueViewProvider: CurrentQueueViewProvider
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
                is Root.MainComponentChild.Player -> playerViewProvider.createPlayerView(
                    component = child.component,
                    config = PlayerView.Config(
                        emptyScreenAction = {},
                        emptyScreenTitle = "No songs in current playlist",
                        emptyScreenMessage = "Select some track for playing",
                        emptyScreenActionTitle = "Select from catalog",
                        coverFallbackIcon = -1 // TODO
                    )
                ).Draw(Modifier.fillMaxSize())
            }
        }
    }

    @Composable
    private fun DrawPages() {
        val pages = component.pages.subscribeAsState()
        val isCatalogActive = pages.value.active.instance is Root.PageComponentChild.Catalog
        val isScanning by component.isScanning.collectAsState()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            floatingActionButton = {
                if (isCatalogActive) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(56.dp))
                    } else {
                        FloatingActionButton(
                            onClick = {
                                val chooser = JFileChooser()
                                chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                chooser.dialogTitle = "Select Music Folder"
                                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    component.addCatalogFolder(chooser.selectedFile)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CreateNewFolder,
                                contentDescription = "Add music folder"
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Column {
                    playerViewProvider.createSmallPlayerView(component.playerComponent).Draw(Modifier)

                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        NavigationBarItem(
                            selected = pages.value.active.instance is Root.PageComponentChild.Queue,
                            onClick = { component.selectPage(0) },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                                    contentDescription = "Playlist"
                                )
                            },
                            label = {
                                Text(
                                    text = "Playlist",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            },
                        )

                        NavigationBarItem(
                            selected = isCatalogActive,
                            onClick = { component.selectPage(1) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.LibraryMusic,
                                    contentDescription = "Library"
                                )
                            },
                            label = {
                                Text(
                                    text = "Library",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            },
                        )
                    }
                }
            }
        ) { paddings ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddings)
            ) {
                Children(
                    stack = component.pages,
                    animation = stackAnimation(animator = scale(frontFactor = 0.8f, backFactor = 0.8f) + fade())
                ) {
                    when (val child = it.instance) {
                        is Root.PageComponentChild.Catalog -> catalogViewProvider.createRootView(child.component)
                        is Root.PageComponentChild.Queue -> currentQueueViewProvider.createCurrentQueueView(child.component)
                    }.Draw(Modifier)
                }
            }
        }
    }
}
