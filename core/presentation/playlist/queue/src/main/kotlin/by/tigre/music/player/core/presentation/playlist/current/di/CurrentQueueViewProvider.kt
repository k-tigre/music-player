package by.tigre.music.player.core.presentation.playlist.current.di

import by.tigre.music.player.core.presentation.playlist.current.component.CurrentQueueComponent
import by.tigre.music.player.core.presentation.playlist.current.view.CurrentQueueView
import by.tigre.music.player.tools.platform.compose.ComposableView

interface CurrentQueueViewProvider {
    fun createCurrentQueueView(component: CurrentQueueComponent): ComposableView

    class Impl : CurrentQueueViewProvider {
        override fun createCurrentQueueView(component: CurrentQueueComponent): CurrentQueueView = CurrentQueueView(component)
    }
}
