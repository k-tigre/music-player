package by.tigre.music.player.desktop.presentation.root.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import by.tigre.music.player.core.presentation.catalog.di.CatalogViewProvider
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueViewProvider
import by.tigre.music.player.desktop.presentation.root.component.Root
import by.tigre.music.player.desktop.presentation.root.view.component.DesktopTab
import by.tigre.music.player.desktop.presentation.root.view.component.DesktopTitleBar
import by.tigre.music.player.desktop.presentation.theme.DesktopBorder
import by.tigre.music.player.desktop.presentation.theme.DesktopGreen
import by.tigre.music.player.desktop.presentation.theme.DesktopSubText
import by.tigre.music.player.desktop.presentation.theme.DesktopTabBg
import by.tigre.music.player.desktop.resources.Res
import by.tigre.music.player.desktop.resources.desktop_tab_library
import by.tigre.music.player.desktop.resources.desktop_tab_queue
import by.tigre.music.player.desktop.resources.desktop_window_title_library
import by.tigre.music.player.tools.platform.compose.resources.Res as ToolsRes
import by.tigre.music.player.tools.platform.compose.resources.cd_add_music_folder
import org.jetbrains.compose.resources.stringResource
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import javax.swing.JFileChooser

@Composable
internal fun LibraryWindowContent(
    component: Root,
    catalogViewProvider: CatalogViewProvider,
    currentQueueViewProvider: CurrentQueueViewProvider,
    windowState: WindowState,
    onClose: () -> Unit,
) {
    // Library title bar moves only itself; capture start pos locally.
    val startPos = remember { floatArrayOf(0f, 0f) }

    val pages = component.pages.subscribeAsState()
    val isCatalogActive = pages.value.active.instance is Root.PageComponentChild.Catalog
    val isScanning by component.isScanning.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Custom title bar (draggable) ─────────────────────────────────────
        DesktopTitleBar(
            title = stringResource(Res.string.desktop_window_title_library),
            onDragStart = {
                val pos = windowState.position as? WindowPosition.Absolute
                startPos[0] = pos?.x?.value ?: 0f
                startPos[1] = pos?.y?.value ?: 0f
            },
            onDrag = { dx, dy ->
                windowState.position = WindowPosition.Absolute(
                    x = (startPos[0] + dx).dp,
                    y = (startPos[1] + dy).dp,
                )
            },
            onClose = onClose,
        )

        // ── Tab bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(DesktopTabBg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DesktopTab(
                title = stringResource(Res.string.desktop_tab_queue),
                selected = !isCatalogActive,
                onClick = { component.selectPage(0) }
            )
            DesktopTab(
                title = stringResource(Res.string.desktop_tab_library),
                selected = isCatalogActive,
                onClick = { component.selectPage(1) }
            )

            Spacer(Modifier.weight(1f))

            if (isCatalogActive) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(20.dp),
                        color = DesktopGreen,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            val chooser = JFileChooser()
                            chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                component.addCatalogFolder(chooser.selectedFile)
                            }
                        },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CreateNewFolder,
                            contentDescription = stringResource(ToolsRes.string.cd_add_music_folder),
                            tint = DesktopSubText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = DesktopBorder, thickness = 1.dp)

        // ── Page content ─────────────────────────────────────────────────────
        Children(stack = component.pages) {
            when (val child = it.instance) {
                is Root.PageComponentChild.Catalog ->
                    catalogViewProvider.createRootView(child.component)

                is Root.PageComponentChild.Queue ->
                    currentQueueViewProvider.createCurrentQueueView(child.component)
            }.Draw(Modifier.fillMaxSize())
        }
    }
}
