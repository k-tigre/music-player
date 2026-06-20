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
import by.tigre.music.player.core.data.catalog.android.ActivityMediaDeleteHandler
import by.tigre.music.player.core.data.catalog.android.MediaDeleteHandlerRegistry
import by.tigre.music.player.platform.ExternalAudioIntentHandler
import by.tigre.music.player.presentation.background.BackgroundService
import by.tigre.media.platform.presentation.BaseComponentContextImpl
import by.tigre.music.player.presentation.root.component.Root
import by.tigre.music.player.presentation.root.view.RootView
import by.tigre.media.platform.tools.platform.compose.AppTheme
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
        )

        externalAudioIntentHandler = ExternalAudioIntentHandler(
            context = this,
            scope = componentContext,
            graph = graph,
            onExternalAudioOpened = root::dismissDefaultPlayerPrompt,
        )

        setContent {
            AppTheme {
                val currentIntent = rememberUpdatedState(intent)
                LaunchedEffect(currentIntent.value) {
                    externalAudioIntentHandler.handle(currentIntent.value)
                }

                Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    RootView(
                        root,
                        catalogViewProvider = CatalogViewProvider.Impl(),
                        playerViewProvider = PlayerViewProvider.Impl(),
                        currentQueueViewProvider = CurrentQueueViewProvider.Impl(),
                    ).Draw(Modifier)
                }
            }
        }

        initializeController()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::externalAudioIntentHandler.isInitialized) {
            externalAudioIntentHandler.handle(intent)
        }
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
