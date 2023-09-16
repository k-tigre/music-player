package by.tigre.music.player.core.presentation.backgound_player.view

import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.backgound_player.di.PlayerBackgroundDependency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext

interface BackgroundComponent : CoroutineScope {

    val currentSong: StateFlow<Song?>

    fun getPlayer(): PlaybackPlayer

    fun pause()
    fun play()
    fun next()
    fun prev()
    fun stop()

    class Impl(dependency: PlayerBackgroundDependency) : BackgroundComponent {

        private val job = SupervisorJob()
        override val coroutineContext: CoroutineContext = Dispatchers.Main + job

        private val playbackController = dependency.playbackController

        override val currentSong: StateFlow<Song?> = playbackController.currentItem

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
