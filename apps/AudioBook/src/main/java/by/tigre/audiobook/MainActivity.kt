package by.tigre.audiobook

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
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogComponentProvider
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AndroidAudiobookCatalogViewProvider
import by.tigre.audiobook.presentation.background.BackgroundService
import by.tigre.audiobook.presentation.root.component.Root
import by.tigre.audiobook.presentation.root.view.RootView
import by.tigre.audiobook.theme.AppTheme
import by.tigre.audiobook.theme.rememberAudiobookDarkTheme
import by.tigre.music.player.core.presentation.catalog.di.PlayerComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.PlayerViewProvider
import by.tigre.music.player.presentation.base.BaseComponentContextImpl
import com.arkivanov.decompose.defaultComponentContext
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : AppCompatActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val graph = (application as App).graph
        val root = Root.Impl(
            context = BaseComponentContextImpl(defaultComponentContext()),
            playerComponentProvider = PlayerComponentProvider.Impl(graph),
            audiobookCatalogComponentProvider = AudiobookCatalogComponentProvider.Impl(graph),
        )

        setContent {
            val preferences = graph.preferences
            val (darkTheme, toggleNightMode) = rememberAudiobookDarkTheme(preferences)
            AppTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    RootView(
                        root,
                        playerViewProvider = PlayerViewProvider.Impl(),
                        audiobookCatalogViewProvider = AndroidAudiobookCatalogViewProvider(),
                        onToggleNightMode = toggleNightMode,
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
