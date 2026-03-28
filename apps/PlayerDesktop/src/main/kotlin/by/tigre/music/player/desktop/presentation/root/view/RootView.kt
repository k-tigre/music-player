package by.tigre.music.player.desktop.presentation.root.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.WindowState
import by.tigre.music.player.core.presentation.catalog.di.CatalogViewProvider
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueViewProvider
import by.tigre.music.player.desktop.presentation.root.component.Root
import by.tigre.music.player.desktop.presentation.theme.DesktopBg
import by.tigre.music.player.desktop.presentation.theme.DesktopTheme
import by.tigre.music.player.tools.platform.compose.ComposableView

class RootView(
    private val component: Root,
    private val catalogViewProvider: CatalogViewProvider,
    private val currentQueueViewProvider: CurrentQueueViewProvider,
) : ComposableView {

    /** Not used in the desktop dual-window layout. */
    @Composable
    override fun Draw(modifier: Modifier) = Unit

    // ── Player window ────────────────────────────────────────────────────────

    @Composable
    fun DrawPlayerWindow(
        onDragStart: () -> Unit,
        onDrag: (dx: Float, dy: Float) -> Unit,
        libraryVisible: Boolean,
        onToggleLibrary: () -> Unit,
        onClose: () -> Unit,
    ) {
        DesktopTheme {
            Surface(color = DesktopBg, modifier = Modifier.fillMaxSize()) {
                PlayerWindowContent(
                    player = component.playerComponent,
                    libraryVisible = libraryVisible,
                    onToggleLibrary = onToggleLibrary,
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onClose = onClose,
                )
            }
        }
    }

    // ── Library window ───────────────────────────────────────────────────────

    @Composable
    fun DrawLibraryWindow(windowState: WindowState, onClose: () -> Unit) {
        DesktopTheme {
            Surface(color = DesktopBg, modifier = Modifier.fillMaxSize()) {
                LibraryWindowContent(
                    component = component,
                    catalogViewProvider = catalogViewProvider,
                    currentQueueViewProvider = currentQueueViewProvider,
                    windowState = windowState,
                    onClose = onClose,
                )
            }
        }
    }
}
