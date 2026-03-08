package by.tigre.music.player.core.presentation.backgound_player.presentation.component

import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.presentation.backgound_player.di.PlayerBackgroundDependency
import by.tigre.music.player.core.presentation.catalog.component.PlayerItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext

interface BackgroundComponent : CoroutineScope {

    val currentItem: Flow<PlayerItem?>

    fun getPlayer(): PlaybackPlayer

    fun pause()
    fun play()
    fun next()
    fun prev()
    fun stop()

    class Impl(dependency: PlayerBackgroundDependency) : BackgroundComponent {

        private val job = SupervisorJob()
        override val coroutineContext: CoroutineContext = Dispatchers.Main + job

        private val playbackController = dependency.basePlaybackController

        override val currentItem: Flow<PlayerItem?> = playbackController.currentItem

        override fun getPlayer(): PlaybackPlayer {
            return playbackController.player
        }

        override fun pause() {
            playbackController.pause()
        }

        override fun play() {
            playbackController.resume()
        }

        override fun next() {
            playbackController.playNext()
        }

        override fun prev() {
            playbackController.playPrev()
        }

        override fun stop() {
            playbackController.stop()
        }
    }
}
