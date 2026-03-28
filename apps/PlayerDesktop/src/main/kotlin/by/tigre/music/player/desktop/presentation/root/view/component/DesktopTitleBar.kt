package by.tigre.music.player.desktop.presentation.root.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import by.tigre.music.player.desktop.presentation.theme.DesktopCloseBg
import by.tigre.music.player.desktop.presentation.theme.DesktopGreen
import by.tigre.music.player.desktop.presentation.theme.DesktopSubText
import by.tigre.music.player.desktop.presentation.theme.DesktopTitleBg

/**
 * Desktop-style title bar. Reports drag deltas (in AWT logical px) via
 * [onDragStart] and [onDrag] so the caller decides which windows to move.
 * This lets the player window move itself AND the library in the same call,
 * keeping them perfectly in sync even during fast drags.
 */
@Composable
internal fun DesktopTitleBar(
    title: String,
    onDragStart: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(DesktopTitleBg)
            .pointerInput(onDragStart, onDrag) {
                var startMouseX = 0
                var startMouseY = 0

                detectDragGestures(
                    onDragStart = { _ ->
                        val mouse = java.awt.MouseInfo.getPointerInfo().location
                        startMouseX = mouse.x
                        startMouseY = mouse.y
                        onDragStart()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val mouse = java.awt.MouseInfo.getPointerInfo().location
                        onDrag(
                            (mouse.x - startMouseX).toFloat(),
                            (mouse.y - startMouseY).toFloat(),
                        )
                    }
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(10.dp))

        // Green dot accent
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(DesktopGreen, CircleShape)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = title,
            color = DesktopGreen,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace,
        )

        Spacer(Modifier.weight(1f))

        // Close button
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(36.dp)
                .background(DesktopCloseBg)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✕",
                color = DesktopSubText,
                fontSize = 11.sp,
            )
        }
    }
}
