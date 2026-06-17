package by.tigre.media.platform.player.di

import androidx.compose.runtime.Composable
import by.tigre.media.platform.player.component.EqualizerComponent
import by.tigre.media.platform.player.component.PlayerComponent
import by.tigre.media.platform.player.component.SmallPlayerComponent
import by.tigre.media.platform.player.view.EqualizerView
import by.tigre.media.platform.player.view.PlayerView
import by.tigre.media.platform.player.view.SmallPlayerView
import by.tigre.media.platform.tools.platform.compose.ComposableView

interface PlayerViewProvider {
    fun createSmallPlayerView(
        component: SmallPlayerComponent,
        showOrderModeButton: Boolean = true,
    ): ComposableView
    fun createPlayerView(
        component: PlayerComponent,
        config: PlayerView.Config,
        topBarContent: (@Composable () -> Unit)? = null,
        chapterTitleContent: (@Composable (title: String) -> Unit)? = null
    ): ComposableView

    fun createEqualizerView(component: EqualizerComponent, showTopBar: Boolean = true): ComposableView

    class Impl : PlayerViewProvider {
        override fun createSmallPlayerView(
            component: SmallPlayerComponent,
            showOrderModeButton: Boolean,
        ): SmallPlayerView =
            SmallPlayerView(component, showOrderModeButton)

        override fun createPlayerView(
            component: PlayerComponent,
            config: PlayerView.Config,
            topBarContent: (@Composable () -> Unit)?,
            chapterTitleContent: (@Composable (title: String) -> Unit)?,
        ): PlayerView = PlayerView(component, config, topBarContent, chapterTitleContent)

        override fun createEqualizerView(component: EqualizerComponent, showTopBar: Boolean): EqualizerView =
            EqualizerView(component, showTopBar = showTopBar)
    }
}
