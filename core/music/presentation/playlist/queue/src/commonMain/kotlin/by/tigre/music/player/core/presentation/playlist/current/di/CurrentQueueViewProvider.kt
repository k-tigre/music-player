package by.tigre.music.player.core.presentation.playlist.current.di

import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.presentation.playlist.current.component.CurrentQueueComponent
import by.tigre.music.player.core.presentation.playlist.current.view.CurrentQueueView
import by.tigre.media.platform.tools.platform.compose.ComposableView

interface CurrentQueueViewProvider {
    fun createCurrentQueueView(component: CurrentQueueComponent): ComposableView

    class Impl(
        private val albumArtProvider: AlbumArtProvider,
    ) : CurrentQueueViewProvider {
        override fun createCurrentQueueView(component: CurrentQueueComponent): CurrentQueueView =
            CurrentQueueView(component, albumArtProvider)
    }
}
