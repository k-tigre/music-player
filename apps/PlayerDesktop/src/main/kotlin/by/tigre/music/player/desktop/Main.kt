package by.tigre.music.player.desktop

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.CatalogViewProvider
import by.tigre.music.player.core.presentation.catalog.di.PlayerComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.PlayerViewProvider
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueComponentProvider
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueViewProvider
import by.tigre.music.player.desktop.di.DesktopApplicationGraph
import by.tigre.music.player.desktop.presentation.root.component.Root
import by.tigre.music.player.desktop.presentation.root.view.RootView
import by.tigre.music.player.logger.ConsoleLogger
import by.tigre.music.player.logger.Log
import by.tigre.music.player.presentation.base.BaseComponentContextImpl
import by.tigre.music.player.tools.platform.compose.AppTheme
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import javax.swing.SwingUtilities

fun main() {
    val graph = DesktopApplicationGraph.create()

    Log.init(Log.Level.DEBUG, ConsoleLogger())

    val lifecycle = LifecycleRegistry()
    val componentContext = DefaultComponentContext(lifecycle = lifecycle)

    lateinit var root: Root
    SwingUtilities.invokeAndWait {
        root = Root.Impl(
            context = BaseComponentContextImpl(componentContext),
            catalogComponentProvider = CatalogComponentProvider.Impl(graph),
            playerComponentProvider = PlayerComponentProvider.Impl(graph),
            currentQueueComponent = CurrentQueueComponentProvider.Impl(graph),
            onAddFolder = graph::addCatalogFolder,
        )
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Music Player"
        ) {
            AppTheme {
                Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    RootView(
                        root,
                        catalogViewProvider = CatalogViewProvider.Impl(),
                        playerViewProvider = PlayerViewProvider.Impl(),
                        currentQueueViewProvider = CurrentQueueViewProvider.Impl()
                    ).Draw(Modifier)
                }
            }
        }
    }
}
