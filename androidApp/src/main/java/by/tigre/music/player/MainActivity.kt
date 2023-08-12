package by.tigre.music.player

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.CatalogViewProvider
import by.tigre.music.player.core.presentation.catalog.di.PlayerComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.PlayerViewProvider
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueComponentProvider
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueViewProvider
import by.tigre.music.player.presentation.background.BackgroundService
import by.tigre.music.player.presentation.base.BaseComponentContextImpl
import by.tigre.music.player.presentation.root.component.Root
import by.tigre.music.player.presentation.root.view.RootView
import by.tigre.music.player.tools.platform.compose.AppMaterial
import com.arkivanov.decompose.defaultComponentContext
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : AppCompatActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val graph = (application as App).graph
        val root = Root.Impl(
            context = BaseComponentContextImpl(defaultComponentContext()),
            catalogComponentProvider = CatalogComponentProvider.Impl(graph),
            playerComponentProvider = PlayerComponentProvider.Impl(graph),
            currentQueueComponent = CurrentQueueComponentProvider.Impl(graph),
        )

        setContent {
            AppMaterial.AppTheme {
                Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    RootView(
                        root,
                        catalogViewProvider = CatalogViewProvider.Impl(),
                        playerViewProvider = PlayerViewProvider.Impl(),
                        currentQueueViewProvider = CurrentQueueViewProvider.Impl()
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

    override fun onDestroy() {
        releaseController()
        super.onDestroy()
    }
}
