package by.tigre.music.player.desktop.di

import by.tigre.music.player.core.data.catalog.di.CatalogModule
import by.tigre.music.player.core.data.catalog.di.DesktopCatalogModule
import by.tigre.music.player.core.data.playback.di.DesktopBasePlaybackModule
import by.tigre.music.player.core.data.playback.di.PlaybackModule
import by.tigre.music.player.core.data.storage.playback_queue.di.DesktopPlaybackQueueModule
import by.tigre.music.player.core.data.storage.preferences.di.DesktopPreferencesModule
import by.tigre.music.player.core.presentation.catalog.component.BasePlaybackController
import by.tigre.music.player.core.presentation.catalog.component.PlayerItem
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.music.player.core.presentation.catalog.di.PlayerDependency
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueDependency
import by.tigre.music.player.tools.coroutines.CoroutineModule
import kotlinx.coroutines.flow.map
import java.io.File

class DesktopApplicationGraph(
    playbackModule: PlaybackModule,
    private val desktopCatalogModule: DesktopCatalogModule
) : CatalogDependency,
    PlayerDependency,
    CurrentQueueDependency,
    PlaybackModule by playbackModule,
    CatalogModule by desktopCatalogModule {

    override val appPlaybackVolume = playbackModule.appPlaybackVolume

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
                        coverUri = null
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

    suspend fun addCatalogFolder(folder: File) = desktopCatalogModule.addFolder(folder)

    companion object {
        fun create(): DesktopApplicationGraph {
            val preferencesModule = DesktopPreferencesModule()
            val dbDir = File(System.getProperty("user.home"), ".music-player")
            val desktopCatalogModule = DesktopCatalogModule(dbDir)
            val coroutineModule = CoroutineModule.Impl()
            val playbackQueueModule = DesktopPlaybackQueueModule(dbDir, coroutineModule, preferencesModule)
            val basePlaybackModule = DesktopBasePlaybackModule(preferencesModule.preferences)
            val playbackModule =
                PlaybackModule.Impl(coroutineModule, playbackQueueModule, desktopCatalogModule, basePlaybackModule)

            return DesktopApplicationGraph(playbackModule, desktopCatalogModule)
        }
    }
}
