package by.tigre.music.player

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
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.CatalogViewProvider
import by.tigre.media.platform.player.di.PlayerComponentProvider
import by.tigre.media.platform.player.di.PlayerViewProvider
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueComponentProvider
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueViewProvider
import by.tigre.music.player.core.presentation.favorites.di.FavoritesComponentProvider
import by.tigre.music.player.core.presentation.favorites.di.FavoritesViewProvider
import by.tigre.music.player.core.presentation.playlist.library.di.PlaylistsComponentProvider
import by.tigre.music.player.core.presentation.playlist.library.di.PlaylistsViewProvider
import by.tigre.music.player.core.data.catalog.android.ActivityMediaDeleteHandler
import by.tigre.music.player.core.data.catalog.android.MediaDeleteHandlerRegistry
import by.tigre.music.player.core.di.PaywallIntents
import by.tigre.music.player.core.di.PaywallSection
import by.tigre.music.player.platform.ExternalAudioIntentHandler
import by.tigre.music.player.presentation.background.BackgroundService
import by.tigre.media.platform.presentation.BaseComponentContextImpl
import by.tigre.music.player.presentation.root.component.Root
import by.tigre.music.player.presentation.root.view.RootView
import by.tigre.media.platform.tools.platform.compose.AppTheme
import by.tigre.media.platform.tools.platform.compose.resolveDarkTheme
import com.arkivanov.decompose.defaultComponentContext
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : AppCompatActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private lateinit var root: Root.Impl
    private lateinit var externalAudioIntentHandler: ExternalAudioIntentHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MediaDeleteHandlerRegistry.register(ActivityMediaDeleteHandler(this))

        val graph = (application as App).graph
        val componentContext = BaseComponentContextImpl(defaultComponentContext())
        root = Root.Impl(
            context = componentContext,
            dependency = graph,
            catalogComponentProvider = CatalogComponentProvider.Impl(graph),
            playerComponentProvider = PlayerComponentProvider.Impl(graph),
            currentQueueComponent = CurrentQueueComponentProvider.Impl(graph),
            playlistsComponentProvider = PlaylistsComponentProvider.Impl(graph),
            favoritesComponentProvider = FavoritesComponentProvider.Impl(graph),
            paywallRequests = graph.paywallRequests,
            activity = this,
            billingService = graph.billingService,
            onTipCompleted = graph::recordTip,
            onBillingMessage = graph::showBillingMessage,
        )

        externalAudioIntentHandler = ExternalAudioIntentHandler(
            context = this,
            scope = componentContext,
            graph = graph,
            onExternalAudioOpened = root::dismissDefaultPlayerPrompt,
        )

        handlePaywallIntent(intent)

        setContent {
            val themeSettings by graph.themeSettingsStore.state.collectAsState()
            val darkTheme = resolveDarkTheme(themeSettings.mode)

            AppTheme(
                darkTheme = darkTheme,
                dynamicColor = themeSettings.dynamicColor,
                contrast = themeSettings.contrast,
            ) {
                val currentIntent = rememberUpdatedState(intent)
                LaunchedEffect(currentIntent.value) {
                    externalAudioIntentHandler.handle(currentIntent.value)
                    handlePaywallIntent(currentIntent.value)
                }

                Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    RootView(
                        root,
                        catalogViewProvider = CatalogViewProvider.Impl(
                            graph.albumArtProvider,
                            graph.artistArtProvider,
                        ),
                        playerViewProvider = PlayerViewProvider.Impl(),
                        currentQueueViewProvider = CurrentQueueViewProvider.Impl(graph.albumArtProvider),
                        playlistsViewProvider = PlaylistsViewProvider.Impl(graph.albumArtProvider),
                        favoritesViewProvider = FavoritesViewProvider.Impl(
                            graph.albumArtProvider,
                            graph.artistArtProvider,
                        ),
                        playlistRepository = graph.playlistRepository,
                        favoritesRepository = graph.favoritesRepository,
                        playbackController = graph.playbackController,
                        addToPlaylistCoordinator = graph.addToPlaylistCoordinator,
                        eventAnalytics = graph.eventAnalytics,
                        billingMessages = graph.billingMessages,
                        eqCarryNotices = graph.eqProfileController.carryForwardNotices,
                    ).Draw(Modifier)
                }
            }
        }

        initializeController()
    }

    override fun onResume() {
        super.onResume()
        (application as App).graph.maybeLaunchInAppReview(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::externalAudioIntentHandler.isInitialized) {
            externalAudioIntentHandler.handle(intent)
        }
        handlePaywallIntent(intent)
    }

    private fun handlePaywallIntent(intent: Intent?) {
        val feature = PaywallIntents.featureFrom(intent?.getStringExtra(PaywallIntents.EXTRA_FEATURE))
            ?: return
        val source = intent?.getStringExtra(PaywallIntents.EXTRA_SOURCE) ?: feature.name
        (application as App).graph.requestPaywall(
            feature = feature,
            source = source,
            initialSection = PaywallSection.Plans,
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

    override fun onDestroy() {
        MediaDeleteHandlerRegistry.unregister()
        releaseController()
        super.onDestroy()
    }
}
