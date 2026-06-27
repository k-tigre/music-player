package by.tigre.music.player.desktop.di

import by.tigre.music.player.core.data.catalog.di.CatalogModule
import by.tigre.music.player.core.data.catalog.di.DesktopCatalogModule
import by.tigre.media.platform.playback.di.DesktopBasePlaybackModule
import by.tigre.music.player.core.data.playback.di.PlaybackModule
import by.tigre.music.player.core.data.storage.playback_queue.di.DesktopPlaybackQueueModule
import by.tigre.media.platform.preferences.di.DesktopPreferencesModule
import by.tigre.media.platform.player.component.BasePlaybackController
import by.tigre.media.platform.player.component.PlayerItem
import by.tigre.media.platform.player.component.RepeatMode
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.media.platform.player.di.PlayerDependency
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueDependency
import by.tigre.music.player.core.data.favorites.di.FavoritesModule
import by.tigre.music.player.core.presentation.favorites.di.FavoritesDependency
import by.tigre.music.player.core.data.playlist.di.PlaylistModule
import by.tigre.music.player.core.presentation.playlist.library.di.PlaylistsDependency
import by.tigre.media.platform.tools.analytics.LogTracker
import by.tigre.media.platform.tools.analytics.music.MusicAnalyticsModuleImpl
import by.tigre.media.platform.tools.analytics.music.MusicAnalyticsModule
import by.tigre.media.platform.tools.coroutines.CoroutineModule
import kotlinx.coroutines.flow.map
import java.io.File

class DesktopApplicationGraph(
    playbackModule: PlaybackModule,
    playbackQueueModule: DesktopPlaybackQueueModule,
    private val desktopCatalogModule: DesktopCatalogModule,
    analyticsModule: MusicAnalyticsModule,
) : CatalogDependency,
    PlayerDependency,
    CurrentQueueDependency,
    PlaylistsDependency,
    FavoritesDependency,
    PlaylistModule,
    FavoritesModule,
    MusicAnalyticsModule by analyticsModule,
    PlaybackModule by playbackModule,
    CatalogModule by desktopCatalogModule {

    private val playlistModule = PlaylistModule.Impl(playbackQueueModule, desktopCatalogModule)
    private val favoritesModule = FavoritesModule.Impl(playbackQueueModule, desktopCatalogModule)

    override val playlistRepository
        get() = playlistModule.playlistRepository

    override val favoritesRepository
        get() = favoritesModule.favoritesRepository

    override val addToPlaylistCoordinator
        get() = playlistModule.addToPlaylistCoordinator

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
                        coverUri = albumArtProvider.albumArtUri(it.albumId),
                    )
                }
            }
            override val shuffleEnabled = controller.shuffleEnabled
            override val repeatMode = controller.repeatMode.map { it.toUiRepeatMode() }
            override fun playNext() = controller.playNext()
            override fun playPrev() = controller.playPrev()
            override fun pause() = controller.pause()
            override fun resume() = controller.resume()
            override fun stop() = controller.stop()
            override fun toggleShuffle() = controller.toggleShuffle()
            override fun cycleRepeat() = controller.cycleRepeat()
        }
    }

    private fun PlaybackQueueStorage.RepeatMode.toUiRepeatMode(): RepeatMode = when (this) {
        PlaybackQueueStorage.RepeatMode.Off -> RepeatMode.Off
        PlaybackQueueStorage.RepeatMode.All -> RepeatMode.All
        PlaybackQueueStorage.RepeatMode.One -> RepeatMode.One
    }

    suspend fun addCatalogFolder(folder: File) = desktopCatalogModule.addFolder(folder)

    companion object {
        fun create(): DesktopApplicationGraph {
            val preferencesModule = DesktopPreferencesModule()
            val dbDir = File(System.getProperty("user.home"), ".music-player")
            val desktopCatalogModule = DesktopCatalogModule(dbDir, preferencesModule.preferences)
            val coroutineModule = CoroutineModule.Impl()
            val playbackQueueModule = DesktopPlaybackQueueModule(dbDir, coroutineModule, preferencesModule)
            val basePlaybackModule = DesktopBasePlaybackModule(preferencesModule.preferences)
            val playbackModule =
                PlaybackModule.Impl(coroutineModule, playbackQueueModule, desktopCatalogModule, basePlaybackModule)
            val analyticsModule = MusicAnalyticsModuleImpl.create(
                tracker = LogTracker(),
                coroutineModule = coroutineModule,
            )

            return DesktopApplicationGraph(
                playbackModule = playbackModule,
                playbackQueueModule = playbackQueueModule,
                desktopCatalogModule = desktopCatalogModule,
                analyticsModule = analyticsModule
            )
        }
    }
}
