package by.tigre.music.player.core.presentation.catalog.di

import androidx.compose.runtime.Composable
import by.tigre.music.player.core.presentation.catalog.component.PlayerComponent
import by.tigre.music.player.core.presentation.catalog.component.SmallPlayerComponent
import by.tigre.music.player.core.presentation.catalog.view.PlayerView
import by.tigre.music.player.core.presentation.catalog.view.SmallPlayerView
import by.tigre.music.player.tools.platform.compose.ComposableView

interface PlayerViewProvider {
    fun createSmallPlayerView(
        component: SmallPlayerComponent,
        showOrderModeButton: Boolean = true,
    ): ComposableView
    fun createPlayerView(
        component: PlayerComponent,
        config: PlayerView.Config,
        topBarContent: (@Composable () -> Unit)? = null
    ): ComposableView

    class Impl : PlayerViewProvider {
        override fun createSmallPlayerView(
            component: SmallPlayerComponent,
            showOrderModeButton: Boolean,
        ): SmallPlayerView =
            SmallPlayerView(component, showOrderModeButton)

        override fun createPlayerView(
            component: PlayerComponent,
            config: PlayerView.Config,
            topBarContent: (@Composable () -> Unit)?
        ): PlayerView = PlayerView(component, config, topBarContent)
    }
}
