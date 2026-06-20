package by.tigre.music.player.core.di

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import by.tigre.music.player.core.data.catalog.di.AndroidCatalogModule
import by.tigre.music.player.core.data.catalog.di.CatalogModule
import by.tigre.media.platform.playback.di.AndroidBasePlaybackModule
import by.tigre.music.player.core.data.playback.di.PlaybackModule
import by.tigre.music.player.core.data.storage.playback_queue.di.AndroidPlaybackQueueModule
import by.tigre.music.player.core.data.storage.playback_queue.di.PlaybackQueueModule
import by.tigre.media.platform.preferences.Preferences
import by.tigre.media.platform.preferences.di.AndroidPreferencesModule
import by.tigre.music.player.platform.PlayerSettings
import by.tigre.music.player.platform.PlayerSettingsImpl
import by.tigre.music.player.car.MusicCarMediaLibrary
import by.tigre.media.platform.background.car.CarMediaLibrary
import by.tigre.media.platform.background.di.PlayerBackgroundDependency
import by.tigre.media.platform.player.component.BasePlaybackController
import by.tigre.media.platform.player.component.PlayerItem
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.media.platform.player.di.PlayerDependency
import by.tigre.media.platform.player.component.RepeatMode
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.presentation.root.di.RootDependency
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueDependency
import by.tigre.media.platform.tools.analytics.music.MusicAnalyticsModule
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import by.tigre.media.platform.tools.coroutines.CoroutineModule
import by.tigre.music.player.core.data.playback.ActivePlaybackSource
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ApplicationGraph(
    val appContext: Context,
    playbackModule: PlaybackModule,
    playbackQueueModule: PlaybackQueueModule,
    catalogModule: CatalogModule,
    analyticsModule: MusicAnalyticsModule,
    private val preferences: Preferences,
) : CatalogDependency,
    PlayerDependency,
    PlayerBackgroundDependency,
    CurrentQueueDependency,
    RootDependency,
    MusicAnalyticsModule by analyticsModule,
    PlaybackModule by playbackModule,
    PlaybackQueueModule by playbackQueueModule,
    CatalogModule by catalogModule {

    override val appPlaybackVolume = playbackModule.appPlaybackVolume

    override val playerSettings: PlayerSettings by lazy {
        PlayerSettingsImpl(appContext, preferences)
    }

    override val carMediaLibrary: CarMediaLibrary by lazy {
        MusicCarMediaLibrary(
            context = appContext,
            catalog = catalogSource,
            playback = playbackController,
        )
    }

    override val basePlaybackController: BasePlaybackController by lazy {
        val controller = playbackController
        object : BasePlaybackController {
            override val player = controller.player
            override val currentItem = combine(
                controller.nowPlayingOverlay,
                controller.currentItem,
                controller.interruption,
            ) { overlay, song, interruption ->
                when {
                    overlay != null -> PlayerItem(
                        title = overlay.title,
                        subtitle = overlay.sourceLabel ?: "",
                        isExternal = true,
                        canReturnToQueue = interruption != null,
                    )

                    song != null -> PlayerItem(
                        title = song.name,
                        subtitle = "${song.artist}/${song.album}",
                        artist = song.artist,
                        album = song.album,
                        coverUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                            song.albumId.value
                        )
                    )

                    else -> null
                }
            }
            override val shuffleEnabled = controller.shuffleEnabled
            override val repeatMode = controller.repeatMode.map { it.toUiRepeatMode() }
            override fun playNext() {
                if (controller.activePlaybackSource.value is ActivePlaybackSource.Overlay) {
                    eventAnalytics.trackEvent(
                        MusicEvents.Action.ExternalAudioOverlayEnded(MusicEvents.OverlayEndReason.Next)
                    )
                }
                controller.playNext()
            }
            override fun playPrev() = controller.playPrev()
            override fun pause() = controller.pause()
            override fun resume() = controller.resume()
            override fun stop() = controller.stop()
            override fun toggleShuffle() = controller.toggleShuffle()
            override fun cycleRepeat() = controller.cycleRepeat()
            override fun resumeInterruptedSession() {
                if (controller.interruption.value != null) {
                    eventAnalytics.trackEvent(
                        MusicEvents.Action.ExternalAudioOverlayEnded(MusicEvents.OverlayEndReason.ReturnButton)
                    )
                }
                controller.resumeInterruptedSession()
            }
        }
    }

    private fun PlaybackQueueStorage.RepeatMode.toUiRepeatMode(): RepeatMode = when (this) {
        PlaybackQueueStorage.RepeatMode.Off -> RepeatMode.Off
        PlaybackQueueStorage.RepeatMode.All -> RepeatMode.All
        PlaybackQueueStorage.RepeatMode.One -> RepeatMode.One
    }

    companion object {
        fun create(
            context: Context,
            analyticsModule: MusicAnalyticsModule,
        ): ApplicationGraph {
            val preferencesModule = AndroidPreferencesModule(context)
            val catalogModule = AndroidCatalogModule(context, preferencesModule.preferences)
            val coroutineModule = CoroutineModule.Impl()
            val playbackQueueModule = AndroidPlaybackQueueModule(context, coroutineModule, preferencesModule)
            val basePlaybackModule =
                AndroidBasePlaybackModule(context, coroutineModule, preferencesModule.preferences)
            val playbackModule =
                PlaybackModule.Impl(coroutineModule, playbackQueueModule, catalogModule, basePlaybackModule)

            return ApplicationGraph(
                appContext = context.applicationContext,
                playbackModule = playbackModule,
                playbackQueueModule = playbackQueueModule,
                catalogModule = catalogModule,
                analyticsModule = analyticsModule,
                preferences = preferencesModule.preferences,
            )
        }
    }
}
