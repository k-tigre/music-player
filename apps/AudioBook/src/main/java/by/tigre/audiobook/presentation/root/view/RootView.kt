package by.tigre.audiobook.presentation.root.view

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import by.tigre.audiobook.R
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogViewProvider
import by.tigre.audiobook.core.presentation.audiobook_catalog.scan.CatalogScanCoordinator
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.AudiobookChapterSelector
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.CatalogScanProgressBanner
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.formatCatalogScanSummary
import by.tigre.audiobook.nighttimer.NightTimerController
import by.tigre.audiobook.nighttimer.NightTimerSettingsScreen
import by.tigre.audiobook.playback.PlaybackSpeedSettingsScreen
import by.tigre.audiobook.presentation.player.view.AudiobookPlayerTopBar
import by.tigre.audiobook.presentation.root.component.Root
import by.tigre.logger.Log
import by.tigre.media.platform.player.di.PlayerViewProvider
import by.tigre.media.platform.player.view.PlayerView
import by.tigre.media.platform.player.view.SmallPlayerView
import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.media.platform.tools.platform.compose.LocalStatusBarInsetHandled
import by.tigre.media.platform.tools.platform.compose.view.BottomBarContainer
import by.tigre.media.platform.tools.platform.compose.view.LocalBottomBarHeight
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation

class RootView(
    private val component: Root,
    private val nightTimerController: NightTimerController,
    private val audiobookPlaybackController: AudiobookPlaybackController,
    private val playerViewProvider: PlayerViewProvider,
    private val audiobookCatalogViewProvider: AudiobookCatalogViewProvider,
    private val catalogScanCoordinator: CatalogScanCoordinator,
) : ComposableView {

    @Composable
    override fun Draw(modifier: Modifier) {
        val scanUi by catalogScanCoordinator.catalogScanUi.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current
        val completedSummaryText = scanUi.completedSummary?.let { formatCatalogScanSummary(it) }
        var wasScanActive by remember { mutableStateOf(false) }
        val bannerColor = MaterialTheme.colorScheme.secondaryContainer

        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            Log.i(TAG) { "POST_NOTIFICATIONS result granted=$granted" }
        }

        LaunchedEffect(scanUi.active) {
            if (!scanUi.active) return@LaunchedEffect
            val nm = context.getSystemService(NotificationManager::class.java)
            val notificationsEnabled = nm?.areNotificationsEnabled() == true
            Log.i(TAG) {
                "scan active: notificationsEnabled=$notificationsEnabled sdk=${Build.VERSION.SDK_INT}"
            }
            if (Build.VERSION.SDK_INT >= 33) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                Log.i(TAG) { "POST_NOTIFICATIONS granted=$granted" }
                if (!granted) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        LaunchedEffect(scanUi.active, completedSummaryText) {
            if (wasScanActive && !scanUi.active) {
                completedSummaryText?.let { snackbarHostState.showSnackbar(it) }
            }
            wasScanActive = scanUi.active
        }

        Box(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                CatalogScanProgressBanner(
                    scanUi = scanUi,
                    onCancel = catalogScanCoordinator::cancelScan,
                )
                CompositionLocalProvider(
                    LocalStatusBarInsetHandled provides scanUi.active,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        DrawMain()
                    }
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            DrawGettingStartedGuide()
        }

        // After DrawMain so this wins over PlayerBackdropSystemBarEffect (transparent).
        if (scanUi.active) {
            ScanBannerStatusBarEffect(color = bannerColor)
        }
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
                                AudiobookPlayerTopBar(
                                    playerComponent = child.component,
                                    nightTimerController = nightTimerController,
                                    onShowCatalog = component::onShowCatalog,
                                    onOpenNightTimerSettings = component::onOpenNightTimerSettings,
                                    onOpenPlaybackSpeedSettings = component::onOpenPlaybackSpeedSettings,
                                    onShowEqualizer = child.component::showEqualizer,
                                )
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

                is Root.MainComponentChild.PlaybackSpeedSettings ->
                    PlaybackSpeedSettingsScreen(
                        playerComponent = component.playerComponent,
                        onBack = component::onClosePlaybackSpeedSettings,
                    )
            }
        }
    }

    @Composable
    private fun DrawGettingStartedGuide() {
        val showGuide by component.showGettingStartedGuide.collectAsState()

        if (showGuide) {
            AlertDialog(
                onDismissRequest = component::dismissGettingStartedGuide,
                title = { Text(stringResource(R.string.getting_started_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.getting_started_intro))
                        GettingStartedStep(
                            number = 1,
                            text = stringResource(R.string.getting_started_step_add_folder),
                        )
                        GettingStartedStep(
                            number = 2,
                            text = stringResource(R.string.getting_started_step_pick_book),
                        )
                        GettingStartedStep(
                            number = 3,
                            text = stringResource(R.string.getting_started_step_player_controls),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = component::openFolderFromGettingStartedGuide) {
                        Text(stringResource(R.string.getting_started_action_add_folder))
                    }
                },
                dismissButton = {
                    TextButton(onClick = component::dismissGettingStartedGuide) {
                        Text(stringResource(R.string.button_got_it))
                    }
                },
            )
        }
    }

    @Composable
    private fun GettingStartedStep(number: Int, text: String) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "$number.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
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
                applyNavigationBarsPadding = false,
            ) {
                playerViewProvider.createSmallPlayerView(
                    component = component.playerComponent,
                    config = SmallPlayerView.Config(
                        showOrderModeButton = false,
                        extendUnderNavigationBar = true,
                        actionsMode = PlayerView.ActionsMode.SeekButtons,
                        seekBack1MinuteLabel = stringResource(R.string.player_seek_back_1_minute),
                        seekBack15SecondsLabel = stringResource(R.string.player_seek_back_15_seconds),
                        seekForward15SecondsLabel = stringResource(R.string.player_seek_forward_15_seconds),
                        seekForward1MinuteLabel = stringResource(R.string.player_seek_forward_1_minute),
                        seek15SecondsDurationCaption = stringResource(R.string.player_seek_duration_15_seconds),
                        seek1MinuteDurationCaption = stringResource(R.string.player_seek_duration_1_minute),
                    ),
                ).Draw(Modifier)
            }
        }
    }

    private companion object {
        const val TAG = "CatalogScan"
    }
}

/** Paints status bar in banner color; composed after player so it wins over transparent backdrop. */
@Composable
private fun ScanBannerStatusBarEffect(color: Color) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val lightIcons = color.luminance() > 0.5f

    // SideEffect every frame so Theme / PlayerBackdrop cannot override mid-scan.
    SideEffect {
        val activity = view.context as? ComponentActivity ?: return@SideEffect
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, view)
        window.statusBarColor = color.toArgb()
        insetsController.isAppearanceLightStatusBars = lightIcons
    }
}
