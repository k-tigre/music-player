package by.tigre.music.player.core.presentation.catalog.di

import by.tigre.music.player.core.presentation.catalog.component.PlayerComponent
import by.tigre.music.player.core.presentation.catalog.component.SmallPlayerComponent
import by.tigre.music.player.core.presentation.catalog.view.PlayerView
import by.tigre.music.player.core.presentation.catalog.view.SmallPlayerView
import by.tigre.music.player.tools.platform.compose.ComposableView

interface PlayerViewProvider {
    fun createSmallPlayerView(component: SmallPlayerComponent): ComposableView
    fun createPlayerView(component: PlayerComponent): ComposableView

    class Impl : PlayerViewProvider {
        override fun createSmallPlayerView(component: SmallPlayerComponent): SmallPlayerView = SmallPlayerView(component)
        override fun createPlayerView(component: PlayerComponent): PlayerView = PlayerView(component)
    }
}
