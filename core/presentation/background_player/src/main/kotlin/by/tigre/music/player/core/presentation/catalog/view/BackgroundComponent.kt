package by.tigre.music.player.core.presentation.catalog.view

import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.backgound_player.di.PlayerBackgroundDependency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

interface BackgroundComponent : CoroutineScope {

    val currentSong: StateFlow<Song?>
    val state: StateFlow<State>

    fun getPlayer(): PlaybackPlayer

    fun pause()
    fun play()
    fun next()

    class Impl(dependency: PlayerBackgroundDependency) : BackgroundComponent {

        private val job = SupervisorJob()
        override val coroutineContext: CoroutineContext = Dispatchers.Main + job

        private val playbackController = dependency.playbackController
        private val notificationAction = MutableSharedFlow<Action>()

        override val currentSong: StateFlow<Song?> = playbackController.currentItem
        override val state = MutableStateFlow(State.Paused)

        init {
            launch {
//                notificationAction
//                    .collect { action ->
//                        when (action) {
//                            Action.RestoreScreen -> {} // TODO
//                            Action.Stop -> {
//
//                            }
//
//                            Action.Play -> TODO()
//                            Action.Pause -> TODO()
//                        }
//                    }
            }

            launch {
                playbackController.player.state.map {
                    if (it == PlaybackPlayer.State.Playing) State.Playing else State.Paused
                }
                    .distinctUntilChanged()
                    .collect(state)
            }
        }

        override fun getPlayer(): PlaybackPlayer {
            return playbackController.player
        }

        override fun pause() {
//            TODO("Not yet implemented")
        }

        override fun play() {
//            TODO("Not yet implemented")
        }

        override fun next() {
//            TODO("Not yet implemented")
        }
    }

    enum class State {
        Playing, Paused
    }

    enum class Action {
        RestoreScreen, Stop, Play, Pause
    }
}
