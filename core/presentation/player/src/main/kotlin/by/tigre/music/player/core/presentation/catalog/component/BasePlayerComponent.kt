package by.tigre.music.player.core.presentation.catalog.component

import android.content.ContentUris
import android.provider.MediaStore
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.catalog.component.BasePlayerComponent.Position
import by.tigre.music.player.core.presentation.catalog.component.BasePlayerComponent.State
import by.tigre.music.player.core.presentation.catalog.di.PlayerDependency
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.tools.coroutines.extensions.throttleFirst
import by.tigre.music.player.tools.coroutines.extensions.withLatestFrom
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

interface BasePlayerComponent {

    val currentItem: StateFlow<PlayerItem?>
    val position: StateFlow<Position>
    val fraction: StateFlow<Float>
    val state: StateFlow<State>
    val isNormal: StateFlow<Boolean>

    fun pause()
    fun play()
    fun next()
    fun prev()
    fun switchMode(isNormal: Boolean)
    fun seekTo(fraction: Float)

    enum class State {
        Playing, Paused
    }

    data class Position(val current: String, val left: String, val total: String)

}

internal class BasePlayerComponentImpl(
    context: BaseComponentContext,
    dependency: PlayerDependency,
) : BasePlayerComponent, BaseComponentContext by context {
    private val playbackController = dependency.playbackController

    private val seekAction = MutableSharedFlow<Float>(extraBufferCapacity = 1)
    override val currentItem: StateFlow<PlayerItem?> = playbackController.currentItem
        .map { song ->
            song?.let {
                PlayerItem(
                    title = it.name,
                    subtitle = "${it.artist}/${it.album}",
                    coverUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        it.albumId.value
                    )
                )
            }
        }
        .stateIn(this, SharingStarted.WhileSubscribed(), initialValue = null)
    override val state = MutableStateFlow(State.Paused)
    override val position = MutableStateFlow(Position("", "", ""))
    override val fraction = MutableStateFlow(0f)
    override val isNormal: StateFlow<Boolean> = playbackController.orderMode
        .stateIn(this, started = SharingStarted.WhileSubscribed(), initialValue = true)

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
                .collect(fraction)
        }

        launch {
            playbackController.player.progress
                .map {
                    if (it.duration > 0) {
                        Position(
                            current = formatTime(it.position),
                            left = formatTime(-(it.duration - it.position)),
                            total = formatTime(it.duration)
                        )
                    } else {
                        Position(
                            current = formatTime(0),
                            left = formatTime(0),
                            total = formatTime(0),
                        )
                    }
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

    override fun switchMode(isNormal: Boolean) {
        playbackController.setOrderMode(isNormal)
    }

    private companion object {
        private const val FORMAT = "%02d:%02d"
        fun formatTime(time: Long): String =
            abs(time / 1000).let { seconds -> FORMAT.format(seconds / 60, seconds % 60) }.run {
                if (time < 0) "-$this" else this
            }
    }
}
