package by.tigre.audiobook.core.di

import android.content.Context
import android.net.Uri
import by.tigre.audiobook.core.data.audiobook.di.AndroidAudiobookCatalogModule
import by.tigre.audiobook.core.data.audiobook.di.AudiobookCatalogModule
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.data.audiobook_playback.di.AudiobookPlaybackModule
import by.tigre.audiobook.core.data.storage.audiobook_catalog.di.AndroidAudiobookCatalogStorageModule
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogDependency
import by.tigre.audiobook.nighttimer.NightTimerController
import by.tigre.audiobook.nighttimer.createNightTimerController
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

class ApplicationGraph(
    playbackModule: PlaybackModule,
    catalogModule: CatalogModule,
    audiobookCatalogModule: AudiobookCatalogModule,
    audiobookPlaybackModule: AudiobookPlaybackModule,
    val nightTimerController: NightTimerController,
) : CatalogDependency,
    PlayerDependency,
    PlayerBackgroundDependency,
    CurrentQueueDependency,
    AudiobookCatalogDependency,
    PlaybackModule by playbackModule,
    CatalogModule by catalogModule,
    AudiobookCatalogModule by audiobookCatalogModule,
    AudiobookPlaybackModule by audiobookPlaybackModule {

    override val appPlaybackVolume = playbackModule.appPlaybackVolume

    override val basePlaybackController: BasePlaybackController by lazy {
        val controller: AudiobookPlaybackController = audiobookPlaybackController
        object : BasePlaybackController {
            override val player = controller.player
            override val currentItem = combine(
                controller.currentBook,
                controller.currentChapter
            ) { book, chapter ->
                if (book != null && chapter != null) {
                    PlayerItem(
                        title = chapter.title,
                        subtitle = book.title,
                        coverUri = book.coverUri?.let(Uri::parse),
                    )
                } else null
            }
            override val orderMode = flowOf(true)
            override fun playNext() = controller.playNextChapter()
            override fun playPrev() = controller.playPrevChapter()
            override fun pause() = controller.pause()
            override fun resume() = controller.resume()
            override fun stop() = controller.stop()
            override fun setOrderMode(isNormal: Boolean) = Unit
        }
    }

    companion object {
        fun create(context: Context): ApplicationGraph {
            val preferencesModule = AndroidPreferencesModule(context)
            val catalogModule = AndroidCatalogModule(context)
            val coroutineModule = CoroutineModule.Impl()
            val playbackQueueModule = AndroidPlaybackQueueModule(context, coroutineModule, preferencesModule)
            val basePlaybackModule =
                AndroidBasePlaybackModule(context, coroutineModule, preferencesModule.preferences)
            val playbackModule =
                PlaybackModule.Impl(coroutineModule, playbackQueueModule, catalogModule, basePlaybackModule)

            val audiobookStorageModule = AndroidAudiobookCatalogStorageModule(context, coroutineModule)
            val audiobookCatalogModule = AndroidAudiobookCatalogModule(context, audiobookStorageModule)
            val audiobookPlaybackModule = AudiobookPlaybackModule.Impl(
                audiobookCatalogStorageModule = audiobookStorageModule,
                audiobookCatalogModule = audiobookCatalogModule,
                basePlaybackModule = basePlaybackModule,
                coroutineModule = coroutineModule
            )

            val preferences = preferencesModule.preferences
            val appPlaybackVolume = requireNotNull(basePlaybackModule.appPlaybackVolume) {
                "Audiobook requires in-app playback volume"
            }
            val nightTimerController = createNightTimerController(
                context = context.applicationContext,
                preferences = preferences,
                playbackController = audiobookPlaybackModule.audiobookPlaybackController,
                appPlaybackVolume = appPlaybackVolume,
                scope = coroutineModule.scope,
            )

            return ApplicationGraph(
                playbackModule,
                catalogModule,
                audiobookCatalogModule,
                audiobookPlaybackModule,
                nightTimerController,
            )
        }
    }
}
