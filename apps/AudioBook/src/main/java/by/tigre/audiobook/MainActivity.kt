package by.tigre.audiobook

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogComponentProvider
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AndroidAudiobookCatalogViewProvider
import by.tigre.audiobook.presentation.background.BackgroundService
import by.tigre.audiobook.presentation.root.component.Root
import by.tigre.audiobook.presentation.root.view.RootView
import by.tigre.audiobook.theme.AppTheme
import by.tigre.logger.Log
import by.tigre.media.platform.player.di.PlayerComponentProvider
import by.tigre.media.platform.player.di.PlayerViewProvider
import by.tigre.media.platform.presentation.BaseComponentContextImpl
import by.tigre.media.platform.tools.platform.compose.resolveDarkTheme
import com.arkivanov.decompose.defaultComponentContext
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : AppCompatActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG) { "onCreate savedInstanceStateNull=${savedInstanceState == null}" }

        val graph = (application as App).graph
        val root = Root.Impl(
            context = BaseComponentContextImpl(defaultComponentContext()),
            playerComponentProvider = PlayerComponentProvider.Impl(graph),
            audiobookCatalogComponentProvider = AudiobookCatalogComponentProvider.Impl(graph),
            screenAnalytics = graph.screenAnalytics,
            eventAnalytics = graph.eventAnalytics,
            audiobookGuideSettings = graph.audiobookGuideSettings,
        )

        setContent {
            val themeSettings by graph.themeSettingsStore.state.collectAsState()
            AppTheme(
                darkTheme = resolveDarkTheme(themeSettings.mode),
                dynamicColor = themeSettings.dynamicColor,
                contrast = themeSettings.contrast,
            ) {
                Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    RootView(
                        component = root,
                        nightTimerController = graph.nightTimerController,
                        audiobookPlaybackController = graph.audiobookPlaybackController,
                        playerViewProvider = PlayerViewProvider.Impl(),
                        audiobookCatalogViewProvider = AndroidAudiobookCatalogViewProvider(),
                        catalogScanCoordinator = graph.catalogScanCoordinator,
                    ).Draw(Modifier)
                }
            }
        }

        initializeController()
    }

    private fun initializeController() {
        releaseController()
        controllerFuture =
            MediaController.Builder(
                this,
                SessionToken(this, ComponentName(this, BackgroundService::class.java))
            )
                .buildAsync()
    }

    private fun releaseController() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG) { "onStart" }
    }

    override fun onStop() {
        Log.i(TAG) {
            "onStop isFinishing=$isFinishing isChangingConfigurations=$isChangingConfigurations"
        }
        super.onStop()
    }

    override fun onDestroy() {
        Log.w(TAG) {
            "onDestroy isFinishing=$isFinishing isChangingConfigurations=$isChangingConfigurations " +
                "scanActive=${runCatching { (application as App).graph.catalogScanCoordinator.catalogScanUi.value.active }.getOrNull()}"
        }
        releaseController()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "CatalogScan"
    }
}
