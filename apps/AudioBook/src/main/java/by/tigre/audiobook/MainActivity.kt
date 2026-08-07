package by.tigre.audiobook

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import by.tigre.audiobook.core.di.PaywallIntents
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
            entitlementsRepository = graph.entitlementsRepository,
            onPaywallRequest = graph::requestPaywall,
            paywallRequests = graph.paywallRequests,
            activity = this,
            billingService = graph.billingService,
            onTipCompleted = graph::recordTip,
            onBillingMessage = graph::showBillingMessage,
        )

        handlePaywallIntent(intent)

        setContent {
            val themeSettings by graph.themeSettingsStore.state.collectAsState()
            val currentIntent = rememberUpdatedState(intent)
            LaunchedEffect(currentIntent.value) {
                handlePaywallIntent(currentIntent.value)
            }
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
                        librarySpaceRepository = graph.librarySpaceRepository,
                        entitlementsRepository = graph.entitlementsRepository,
                        playerViewProvider = PlayerViewProvider.Impl(),
                        audiobookCatalogViewProvider = AndroidAudiobookCatalogViewProvider(),
                        catalogScanCoordinator = graph.catalogScanCoordinator,
                        billingMessages = graph.billingMessages,
                        eqCarryNotices = graph.eqProfileController.carryForwardNotices,
                    ).Draw(Modifier)
                }
            }
        }

        initializeController()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePaywallIntent(intent)
    }

    private fun handlePaywallIntent(intent: Intent?) {
        val feature = PaywallIntents.featureFrom(intent?.getStringExtra(PaywallIntents.EXTRA_FEATURE))
            ?: return
        val source = intent?.getStringExtra(PaywallIntents.EXTRA_SOURCE) ?: feature.name
        (application as App).graph.requestPaywall(
            feature = feature,
            source = source,
        )
        intent?.removeExtra(PaywallIntents.EXTRA_FEATURE)
        intent?.removeExtra(PaywallIntents.EXTRA_SOURCE)
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
