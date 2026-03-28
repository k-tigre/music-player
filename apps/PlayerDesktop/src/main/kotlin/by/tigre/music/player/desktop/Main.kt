package by.tigre.music.player.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.CatalogViewProvider
import by.tigre.music.player.core.presentation.catalog.di.PlayerComponentProvider
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueComponentProvider
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueViewProvider
import by.tigre.music.player.desktop.di.DesktopApplicationGraph
import by.tigre.music.player.desktop.presentation.root.component.Root
import by.tigre.music.player.desktop.presentation.root.view.RootView
import by.tigre.music.player.logger.ConsoleLogger
import by.tigre.music.player.logger.Log
import by.tigre.music.player.presentation.base.BaseComponentContextImpl
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import javax.swing.SwingUtilities

private val PLAYER_SIZE = DpSize(520.dp, 210.dp)
private val LIBRARY_SIZE = DpSize(520.dp, 500.dp)

// ── Persisted preferences ─────────────────────────────────────────────────────
private object AppPrefs {
    private val prefs = java.util.prefs.Preferences
        .userRoot()
        .node("by.tigre.music.player.desktop")

    var libraryVisible: Boolean
        get() = prefs.getBoolean("library_visible", true)
        set(value) = prefs.putBoolean("library_visible", value)
}

fun main() {
    val graph = DesktopApplicationGraph.create()

    Log.init(Log.Level.DEBUG, ConsoleLogger())

    // Set dock / taskbar icon (macOS + Linux + Windows)
    val appIconImage = createAppIcon(512)
    try {
        val taskbar = java.awt.Taskbar.getTaskbar()
        if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
            taskbar.iconImage = appIconImage
        }
    } catch (_: Exception) { /* Taskbar not supported on this platform */
    }

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

    val rootView = RootView(
        component = root,
        catalogViewProvider = CatalogViewProvider.Impl(),
        currentQueueViewProvider = CurrentQueueViewProvider.Impl(),
    )

    application {
        val iconPainter = remember { BitmapPainter(appIconImage.toComposeImageBitmap()) }

        // ── Persisted state ───────────────────────────────────────────────
        var libraryVisible by remember { mutableStateOf(AppPrefs.libraryVisible) }

        LaunchedEffect(libraryVisible) {
            AppPrefs.libraryVisible = libraryVisible
        }

        // ── Initial positions — computed from usable screen area ──────────
        //
        // WindowPosition.Aligned(Center) lets the OS decide placement, but the
        // OS may offset for the menu bar or dock, making our library estimate
        // slightly off. Using Absolute coordinates for both windows ensures
        // they are computed in the same coordinate space and stay aligned.
        val usableScreen = remember {
            java.awt.GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .maximumWindowBounds
        }

        val playerInitialPos = remember {
            WindowPosition.Absolute(
                x = (usableScreen.x + (usableScreen.width - PLAYER_SIZE.width.value.toInt()) / 2).dp,
                y = (usableScreen.y + (usableScreen.height - PLAYER_SIZE.height.value.toInt()) / 2).dp,
            )
        }

        val libraryInitialPos = remember {
            // Place to the right if there's enough space; otherwise below.
            val screenRight = (usableScreen.x + usableScreen.width).dp
            val spaceRight = screenRight - playerInitialPos.x - PLAYER_SIZE.width
            if (spaceRight >= LIBRARY_SIZE.width) {
                WindowPosition.Absolute(
                    x = playerInitialPos.x + PLAYER_SIZE.width,
                    y = playerInitialPos.y,                     // same Y → aligned
                )
            } else {
                WindowPosition.Absolute(
                    x = playerInitialPos.x,
                    y = playerInitialPos.y + PLAYER_SIZE.height,
                )
            }
        }

        // ── Window states ─────────────────────────────────────────────────
        val playerState = rememberWindowState(
            width = PLAYER_SIZE.width,
            height = PLAYER_SIZE.height,
            position = playerInitialPos,
        )

        // Lives outside the if-block so position is preserved across hide/show.
        val libraryState = rememberWindowState(
            width = LIBRARY_SIZE.width,
            height = LIBRARY_SIZE.height,
            position = libraryInitialPos,
        )

        // ── Main player window ────────────────────────────────────────────
        Window(
            onCloseRequest = ::exitApplication,
            title = "Music Player",
            icon = iconPainter,
            state = playerState,
            resizable = false,
            undecorated = true,
        ) {
            // Snapshot both starting positions on drag start, then move both
            // windows in the same onDrag call — no async lag between them.
            val playerDragStart = remember { floatArrayOf(0f, 0f) }
            val libraryDragStart = remember { floatArrayOf(0f, 0f) }

            rootView.DrawPlayerWindow(
                onDragStart = {
                    playerDragStart[0] = (playerState.position as? WindowPosition.Absolute)?.x?.value ?: 0f
                    playerDragStart[1] = (playerState.position as? WindowPosition.Absolute)?.y?.value ?: 0f
                    libraryDragStart[0] = (libraryState.position as? WindowPosition.Absolute)?.x?.value ?: 0f
                    libraryDragStart[1] = (libraryState.position as? WindowPosition.Absolute)?.y?.value ?: 0f
                },
                onDrag = { dx, dy ->
                    playerState.position = WindowPosition.Absolute(
                        x = (playerDragStart[0] + dx).dp,
                        y = (playerDragStart[1] + dy).dp,
                    )
                    if (libraryVisible) {
                        libraryState.position = WindowPosition.Absolute(
                            x = (libraryDragStart[0] + dx).dp,
                            y = (libraryDragStart[1] + dy).dp,
                        )
                    }
                },
                libraryVisible = libraryVisible,
                onToggleLibrary = { libraryVisible = !libraryVisible },
                onClose = ::exitApplication,
            )
        }

        // ── Library window — hides on close, drags independently ──────────
        if (libraryVisible) {
            Window(
                onCloseRequest = { libraryVisible = false },
                title = "Library",
                icon = iconPainter,
                state = libraryState,
                undecorated = true,
            ) {
                rootView.DrawLibraryWindow(
                    windowState = libraryState,
                    onClose = { libraryVisible = false },
                )
            }
        }
    }
}
