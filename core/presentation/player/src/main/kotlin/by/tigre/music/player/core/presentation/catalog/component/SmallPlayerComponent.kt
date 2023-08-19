package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.catalog.di.PlayerDependency
import by.tigre.music.player.core.presentation.catalog.navigation.PlayerNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.tools.coroutines.extensions.throttleFirst
import by.tigre.music.player.tools.coroutines.extensions.withLatestFrom
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface SmallPlayerComponent {

    val currentSong: StateFlow<Song?>
    val position: StateFlow<Float>
    val state: StateFlow<State>

    fun pause()
    fun play()
    fun next()
    fun prev()
    fun seekTo(fraction: Float)
    fun showQueue()

    class Impl(
        context: BaseComponentContext,
        dependency: PlayerDependency,
        private val navigator: PlayerNavigator
    ) : SmallPlayerComponent, BaseComponentContext by context {
        private val playbackController = dependency.playbackController

        private val seekAction = MutableSharedFlow<Float>(extraBufferCapacity = 1)
        override val currentSong: StateFlow<Song?> = playbackController.currentItem
        override val state = MutableStateFlow(State.Paused)
        override val position = MutableStateFlow(0f)

        init {
            launch {
                playbackController.player.state.map {
                    if (it == PlaybackPlayer.State.Playing) State.Playing else State.Paused
                }
                    .distinctUntilChanged()
                    .collect(state)
            }

            launch {
                playbackController.player.progress
                    .map {
                        if (it.duration > 0) it.position.toFloat() / it.duration else 0f
                    }
                    .collect(position)
            }

            launch {
                seekAction
                    .throttleFirst(100)
                    .withLatestFrom(playbackController.player.progress) { fraction, progress ->
                        (fraction * progress.duration).toLong()
                    }
                    .collect {
                        playbackController.player.seekTo(it)
                    }
            }
        }

        override fun seekTo(fraction: Float) {
            seekAction.tryEmit(fraction)
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

        override fun showQueue() {
            navigator.showQueue()
        }
    }

    enum class State {
        Playing, Paused
    }
}
