package by.tigre.audiobook.presentation.root.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import by.tigre.audiobook.R
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogViewProvider
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.AudiobookChapterSelector
import by.tigre.audiobook.nighttimer.NightTimerController
import by.tigre.audiobook.nighttimer.NightTimerSettingsScreen
import by.tigre.audiobook.presentation.root.component.Root
import by.tigre.music.player.core.presentation.catalog.di.PlayerViewProvider
import by.tigre.music.player.core.presentation.catalog.view.PlayerView
import by.tigre.music.player.tools.platform.compose.ComposableView
import by.tigre.music.player.tools.platform.compose.view.BottomBarContainer
import by.tigre.music.player.tools.platform.compose.view.LocalBottomBarHeight
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation

class RootView(
    private val component: Root,
    private val nightTimerController: NightTimerController,
    private val audiobookPlaybackController: AudiobookPlaybackController,
    private val playerViewProvider: PlayerViewProvider,
    private val audiobookCatalogViewProvider: AudiobookCatalogViewProvider,
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
                is Root.MainComponentChild.Player -> {
                    val showFinishedOverlay by audiobookPlaybackController.bookFinishedBannerVisible.collectAsState()
                    Box(modifier = Modifier.fillMaxSize()) {
                        playerViewProvider.createPlayerView(
                            component = child.component,
                            config = PlayerView.Config(
                                emptyScreenAction = component::onShowCatalog,
                                emptyScreenTitle = "No book selected",
                                emptyScreenMessage = "Select a book to listen to",
                                emptyScreenActionTitle = "Select from catalog",
                                coverFallbackIcon = R.drawable.ic_launcher_foreground,
                                showOrderModeButton = false,
                                actionsMode = PlayerView.ActionsMode.SeekButtons,
                                seekBack1MinuteLabel = stringResource(R.string.player_seek_back_1_minute),
                                seekBack15SecondsLabel = stringResource(R.string.player_seek_back_15_seconds),
                                seekForward15SecondsLabel = stringResource(R.string.player_seek_forward_15_seconds),
                                seekForward1MinuteLabel = stringResource(R.string.player_seek_forward_1_minute),
                                seek15SecondsDurationCaption = stringResource(R.string.player_seek_duration_15_seconds),
                                seek1MinuteDurationCaption = stringResource(R.string.player_seek_duration_1_minute),
                                equalizerMenuLabel = stringResource(R.string.player_equalizer_menu),
                                queueMenuLabel = stringResource(R.string.player_queue_menu),
                            ),
                            chapterTitleContent = { chapterTitle ->
                                AudiobookChapterSelector(
                                    controller = audiobookPlaybackController,
                                    chapterTitle = chapterTitle,
                                )
                            },
                            topBarContent = {
                                val eqAvailable by child.component.playbackEqualizer.isAvailable.collectAsState()
                                val nightUi by nightTimerController.uiState.collectAsState()
                                var menuExpanded by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = if (nightUi.isRunning) {
                                        Arrangement.SpaceBetween
                                    } else {
                                        Arrangement.End
                                    },
                                ) {
                                    if (nightUi.isRunning) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = stringResource(
                                                    R.string.night_timer_countdown,
                                                    nightUi.remainingSeconds / 60,
                                                    nightUi.remainingSeconds % 60,
                                                ),
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                            IconButton(onClick = nightTimerController::cancelTimer) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = stringResource(R.string.night_timer_cancel_cd),
                                                )
                                            }
                                        }
                                    }
                                    Box {
                                        IconButton(
                                            onClick = { menuExpanded = true },
                                        ) {
                                            Icon(
                                                contentDescription = stringResource(R.string.player_overflow_menu_cd),
                                                imageVector = Icons.Default.MoreVert,
                                                modifier = Modifier.size(56.dp),
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false },
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.player_menu_settings)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    component.onOpenFolderSettings()
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.player_menu_library)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    component.onShowCatalog()
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.player_menu_night_timer)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    component.onOpenNightTimerSettings()
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.player_equalizer_menu)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    if (eqAvailable) child.component.showEqualizer()
                                                },
                                                enabled = eqAvailable,
                                            )
                                        }
                                    }
                                }
                            },
                        ).Draw(Modifier.fillMaxSize())

                        if (showFinishedOverlay) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 24.dp),
                                color = MaterialTheme.colorScheme.inverseSurface,
                                tonalElevation = 4.dp,
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    text = stringResource(R.string.player_book_finished_message),
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                )
                            }
                        }
                    }
                }

                is Root.MainComponentChild.Equalizer ->
                    playerViewProvider.createEqualizerView(child.component).Draw(Modifier.fillMaxSize())

                is Root.MainComponentChild.NightTimerSettings ->
                    NightTimerSettingsScreen(
                        controller = nightTimerController,
                        onBack = component::onCloseNightTimerSettings,
                    )
            }
        }
    }

    @Composable
    private fun DrawPages() {
        var bottomBarHeight by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current

        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalBottomBarHeight provides bottomBarHeight) {
                audiobookCatalogViewProvider.createRootView(component.audiobookCatalogComponent)
                    .Draw(Modifier.fillMaxSize())
            }

            BottomBarContainer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        bottomBarHeight = with(density) { size.height.toDp() }
                    },
            ) {
                playerViewProvider.createSmallPlayerView(
                    component = component.playerComponent,
                    showOrderModeButton = false,
                ).Draw(Modifier)
            }
        }
    }
}
