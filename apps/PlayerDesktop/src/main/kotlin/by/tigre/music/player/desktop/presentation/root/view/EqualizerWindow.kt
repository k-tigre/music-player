package by.tigre.music.player.desktop.presentation.root.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import by.tigre.music.player.core.presentation.catalog.component.EqualizerComponent
import by.tigre.music.player.core.presentation.catalog.di.PlayerViewProvider
import by.tigre.music.player.desktop.presentation.root.view.component.DesktopTitleBar
import by.tigre.music.player.desktop.presentation.theme.DesktopBorder
import by.tigre.music.player.desktop.resources.Res
import by.tigre.music.player.desktop.resources.desktop_window_title_equalizer
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EqualizerWindowContent(
    component: EqualizerComponent,
    playerViewProvider: PlayerViewProvider,
    windowState: WindowState,
) {
    val startPos = remember { floatArrayOf(0f, 0f) }

    Column(modifier = Modifier.fillMaxSize()) {
        DesktopTitleBar(
            title = stringResource(Res.string.desktop_window_title_equalizer),
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
            onClose = component::close,
        )

        HorizontalDivider(color = DesktopBorder, thickness = 1.dp)

        playerViewProvider.createEqualizerView(component, showTopBar = false)
            .Draw(Modifier.fillMaxSize())
    }
}
