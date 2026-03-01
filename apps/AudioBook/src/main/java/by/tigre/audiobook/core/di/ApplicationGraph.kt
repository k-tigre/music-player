package by.tigre.audiobook.core.di

import android.content.Context
import by.tigre.audiobook.core.data.audiobook.di.AudiobookCatalogModule
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.data.audiobook_playback.di.AudiobookPlaybackModule
import by.tigre.audiobook.core.data.storage.audiobook_catalog.di.AudiobookCatalogStorageModule
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogDependency
import by.tigre.music.player.core.data.catalog.di.CatalogModule
import by.tigre.music.player.core.data.playback.di.PlaybackModule
import by.tigre.music.player.core.data.storage.playback_queue.di.PlaybackQueueModule
import by.tigre.music.player.core.data.storage.preferences.di.PreferencesModule
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
    audiobookPlaybackModule: AudiobookPlaybackModule
) : CatalogDependency,
    PlayerDependency,
    PlayerBackgroundDependency,
    CurrentQueueDependency,
    AudiobookCatalogDependency,
    PlaybackModule by playbackModule,
    CatalogModule by catalogModule,
    AudiobookCatalogModule by audiobookCatalogModule,
    AudiobookPlaybackModule by audiobookPlaybackModule {

    override val basePlaybackController: BasePlaybackController by lazy {
        val controller: AudiobookPlaybackController = audiobookPlaybackController
        object : BasePlaybackController {
            override val player = controller.player
            override val currentItem = combine(
                controller.currentBook,
                controller.currentChapter
            ) { book, chapter ->
                if (book != null && chapter != null) {
                    PlayerItem(title = chapter.title, subtitle = book.title)
                } else null
            }
            override val orderMode = flowOf(true)
            override fun playNext() = controller.playNextChapter()
            override fun playPrev() = controller.playPrevChapter()
            override fun pause() = controller.pause()
            override fun resume() = controller.resume()
            override fun setOrderMode(isNormal: Boolean) = Unit
        }
    }

    companion object {
        fun create(context: Context): ApplicationGraph {
            val preferencesModule = PreferencesModule.Impl(context)
            val catalogModule = CatalogModule.Impl(context)
            val coroutineModule = CoroutineModule.Impl()
            val playbackQueueModule = PlaybackQueueModule.Impl(context, coroutineModule, preferencesModule)
            val playbackModule = PlaybackModule.Impl(context, coroutineModule, playbackQueueModule, catalogModule)

            val audiobookStorageModule = AudiobookCatalogStorageModule.Impl(context, coroutineModule)
            val audiobookCatalogModule = AudiobookCatalogModule.Impl(context, audiobookStorageModule)
            val audiobookPlaybackModule = AudiobookPlaybackModule.Impl(
                audiobookCatalogStorageModule = audiobookStorageModule,
                audiobookCatalogModule = audiobookCatalogModule,
                playbackModule = playbackModule,
                coroutineModule = coroutineModule
            )

            return ApplicationGraph(playbackModule, catalogModule, audiobookCatalogModule, audiobookPlaybackModule)
        }
    }
}
