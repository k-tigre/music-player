package by.tigre.music.player.core.di

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import by.tigre.music.player.core.data.catalog.di.AndroidCatalogModule
import by.tigre.music.player.core.data.catalog.di.CatalogModule
import by.tigre.music.player.core.data.playback.di.AndroidBasePlaybackModule
import by.tigre.music.player.core.data.playback.di.PlaybackModule
import by.tigre.music.player.core.data.storage.playback_queue.di.AndroidPlaybackQueueModule
import by.tigre.music.player.core.data.storage.preferences.di.AndroidPreferencesModule
import by.tigre.music.player.core.presentation.backgound_player.di.PlayerBackgroundDependency
import by.tigre.music.player.core.presentation.catalog.component.BasePlaybackController
import by.tigre.music.player.core.presentation.catalog.component.PlayerItem
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.music.player.core.presentation.catalog.di.PlayerDependency
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueDependency
import by.tigre.music.player.tools.coroutines.CoroutineModule
import kotlinx.coroutines.flow.map

class ApplicationGraph(
    playbackModule: PlaybackModule,
    catalogModule: CatalogModule
) : CatalogDependency,
    PlayerDependency,
    PlayerBackgroundDependency,
    CurrentQueueDependency,
    PlaybackModule by playbackModule,
    CatalogModule by catalogModule {

    override val basePlaybackController: BasePlaybackController by lazy {
        val controller = playbackController
        object : BasePlaybackController {
            override val player = controller.player
            override val currentItem = controller.currentItem.map { song ->
                song?.let {
                    PlayerItem(
                        title = it.name,
                        subtitle = "${it.artist}/${it.album}",
                        artist = it.artist,
                        album = it.album,
                        coverUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                            it.albumId.value
                        )
                    )
                }
            }
            override val orderMode = controller.orderMode
            override fun playNext() = controller.playNext()
            override fun playPrev() = controller.playPrev()
            override fun pause() = controller.pause()
            override fun resume() = controller.resume()
            override fun stop() = controller.stop()
            override fun setOrderMode(isNormal: Boolean) = controller.setOrderMode(isNormal)
        }
    }

    companion object {
        fun create(context: Context): ApplicationGraph {
            val preferencesModule = AndroidPreferencesModule(context)
            val catalogModule = AndroidCatalogModule(context)
            val coroutineModule = CoroutineModule.Impl()
            val playbackQueueModule = AndroidPlaybackQueueModule(context, coroutineModule, preferencesModule)
            val basePlaybackModule = AndroidBasePlaybackModule(context, coroutineModule)
            val playbackModule =
                PlaybackModule.Impl(coroutineModule, playbackQueueModule, catalogModule, basePlaybackModule)

            return ApplicationGraph(playbackModule, catalogModule)
        }
    }
}
