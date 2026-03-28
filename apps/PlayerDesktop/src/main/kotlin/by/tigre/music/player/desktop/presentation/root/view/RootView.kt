package by.tigre.music.player.desktop.presentation.root.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import by.tigre.music.player.core.presentation.catalog.component.BasePlayerComponent
import by.tigre.music.player.core.presentation.catalog.di.CatalogViewProvider
import by.tigre.music.player.core.presentation.catalog.view.PlayerProgressSlider
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueViewProvider
import by.tigre.music.player.desktop.presentation.root.component.Root
import by.tigre.music.player.tools.platform.compose.ComposableView
import coil3.compose.AsyncImage
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import javax.swing.JFileChooser

// ── Desktop colour palette ────────────────────────────────────────────────────
private val DesktopBg = Color(0xFF1C1C1C)
private val DesktopPanel = Color(0xFF252525)
private val DesktopBorder = Color(0xFF3A3A3A)
private val DesktopGreen = Color(0xFF00CC44)
private val DesktopText = Color(0xFFDDDDDD)
private val DesktopSubText = Color(0xFF888888)
private val DesktopTitleBg = Color(0xFF141414)

private val DesktopColorScheme = darkColorScheme(
    background = DesktopBg,
    surface = DesktopPanel,
    surfaceVariant = Color(0xFF2E2E2E),
    surfaceContainerLow = DesktopPanel,
    surfaceContainerHighest = Color(0xFF2E2E2E),
    primary = DesktopGreen,
    secondary = DesktopGreen,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = DesktopText,
    onSurface = DesktopText,
    outline = DesktopBorder,
)

@Composable
private fun DesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DesktopColorScheme, content = content)
}

// ── Root view ────────────────────────────────────────────────────────────────

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
                val player = component.playerComponent
                val current by player.currentItem.collectAsState()
                val state by player.state.collectAsState()
                val fraction by player.fraction.collectAsState()
                val position by player.position.collectAsState()
                val isNormal by player.isNormal.collectAsState()

                Column(modifier = Modifier.fillMaxSize()) {
                    // ── Custom title bar (draggable) ─────────────────────
                    DesktopTitleBar(
                        title = "MUSIC PLAYER",
                        onDragStart = onDragStart,
                        onDrag = onDrag,
                        onClose = onClose,
                    )

                    HorizontalDivider(color = DesktopBorder, thickness = 1.dp)

                    // ── Cover + info ─────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2A2A2A)),
                            contentAlignment = Alignment.Center
                        ) {
                            val uri = current?.coverUri
                            if (uri != null) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = DesktopGreen.copy(alpha = 0.45f),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = current?.title ?: "No track",
                                color = DesktopText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = current?.subtitle ?: "---",
                                color = DesktopSubText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // ── Seek slider ──────────────────────────────────────
                    PlayerProgressSlider(
                        fraction = fraction,
                        onSeekTo = player::seekTo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-10).dp)
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = position.current,
                            color = DesktopGreen,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = position.left,
                            color = DesktopSubText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // ── Transport controls ───────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { player.switchMode(!isNormal) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isNormal) Icons.Default.Repeat else Icons.Default.Shuffle,
                                contentDescription = null,
                                tint = if (isNormal) DesktopSubText else DesktopGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(Modifier.width(4.dp))

                        IconButton(
                            onClick = player::prev,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = null,
                                tint = DesktopText,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Play / Pause — circular green button
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DesktopGreen)
                                .clickable {
                                    if (state == BasePlayerComponent.State.Playing) player.pause()
                                    else player.play()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state == BasePlayerComponent.State.Playing)
                                    Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        IconButton(
                            onClick = player::next,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null,
                                tint = DesktopText,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(Modifier.width(4.dp))

                        // [PL] toggle library window
                        val plBorder = if (libraryVisible) DesktopGreen else DesktopBorder
                        val plBg = if (libraryVisible) DesktopGreen.copy(alpha = 0.15f) else Color(0xFF2A2A2A)
                        val plColor = if (libraryVisible) DesktopGreen else DesktopSubText

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(plBg)
                                .border(1.dp, plBorder, RoundedCornerShape(2.dp))
                                .clickable(onClick = onToggleLibrary)
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PL",
                                color = plColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Library window ───────────────────────────────────────────────────────

    @Composable
    fun DrawLibraryWindow(windowState: WindowState, onClose: () -> Unit) {
        // Library title bar moves only itself; capture start pos locally.
        val startPos = remember { floatArrayOf(0f, 0f) }

        DesktopTheme {
            Surface(color = DesktopBg, modifier = Modifier.fillMaxSize()) {
                val pages = component.pages.subscribeAsState()
                val isCatalogActive = pages.value.active.instance is Root.PageComponentChild.Catalog
                val isScanning by component.isScanning.collectAsState()

                Column(modifier = Modifier.fillMaxSize()) {
                    // ── Custom title bar (draggable) ─────────────────────
                    DesktopTitleBar(
                        title = "LIBRARY",
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

                    // ── Tab bar ──────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(Color(0xFF181818)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DesktopTab(
                            title = "QUEUE",
                            selected = !isCatalogActive,
                            onClick = { component.selectPage(0) }
                        )
                        DesktopTab(
                            title = "LIBRARY",
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
                                        contentDescription = "Add music folder",
                                        tint = DesktopSubText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DesktopBorder, thickness = 1.dp)

                    // ── Page content ─────────────────────────────────────
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
        }
    }
}

// ── Shared composables ───────────────────────────────────────────────────────

/**
 * Desktop-style title bar. Reports drag deltas (in AWT logical px) via
 * [onDragStart] and [onDrag] so the caller decides which windows to move.
 * This lets the player window move itself AND the library in the same call,
 * keeping them perfectly in sync even during fast drags.
 */
@Composable
private fun DesktopTitleBar(
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
                .background(Color(0xFF1E1E1E))
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

@Composable
private fun DesktopTab(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .background(if (selected) DesktopPanel else Color(0xFF181818))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) DesktopGreen else DesktopSubText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}
