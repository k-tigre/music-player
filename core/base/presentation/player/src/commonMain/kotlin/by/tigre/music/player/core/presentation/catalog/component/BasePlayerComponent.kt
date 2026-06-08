package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.data.playback.AppPlaybackVolume
import by.tigre.music.player.core.data.playback.PlaybackEqualizer
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.presentation.catalog.component.BasePlayerComponent.Position
import by.tigre.music.player.core.presentation.catalog.component.BasePlayerComponent.State
import by.tigre.music.player.core.presentation.catalog.di.PlayerDependency
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.tools.analytics.Event
import by.tigre.music.player.tools.analytics.EventAnalytics
import by.tigre.music.player.tools.coroutines.extensions.throttleFirst
import by.tigre.music.player.tools.coroutines.extensions.withLatestFrom
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

interface BasePlayerComponent {

    val currentItem: StateFlow<PlayerItem?>
    val position: StateFlow<Position>
    val fraction: StateFlow<Float>
    val state: StateFlow<State>
    val isNormal: StateFlow<Boolean>
    val playbackEqualizer: PlaybackEqualizer
    val appPlaybackVolume: AppPlaybackVolume?

    fun pause()
    fun play()
    fun next()
    fun prev()
    fun seekBack15Seconds()
    fun seekBack1Minute()
    fun seekForward15Seconds()
    fun seekForward1Minute()
    fun switchMode(isNormal: Boolean)
    fun seekTo(fraction: Float)
    fun onSeekCommitted(fraction: Float) = Unit

    enum class State {
        Playing, Paused
    }

    data class Position(val current: String, val left: String, val total: String)

}

internal class BasePlayerComponentImpl(
    context: BaseComponentContext,
    dependency: PlayerDependency,
) : BasePlayerComponent, BaseComponentContext by context {
    private val basePlaybackController = dependency.basePlaybackController
    private val eventAnalytics: EventAnalytics = dependency.eventAnalytics

    override val playbackEqualizer: PlaybackEqualizer = dependency.playbackEqualizer

    override val appPlaybackVolume: AppPlaybackVolume? = dependency.appPlaybackVolume

    private val seekAction = MutableSharedFlow<Float>(extraBufferCapacity = 1)
    override val currentItem: StateFlow<PlayerItem?> = basePlaybackController.currentItem
        .stateIn(this, SharingStarted.WhileSubscribed(), initialValue = null)
    override val state = MutableStateFlow(State.Paused)
    override val position = MutableStateFlow(Position("", "", ""))
    override val fraction = MutableStateFlow(0f)
    override val isNormal: StateFlow<Boolean> = basePlaybackController.orderMode
        .stateIn(this, started = SharingStarted.WhileSubscribed(), initialValue = true)

    init {
        launch {
            basePlaybackController.player.state.map {
                if (it == PlaybackPlayer.State.Playing) State.Playing else State.Paused
            }
                .distinctUntilChanged()
                .collect(state)
        }

        launch {
            basePlaybackController.player.progress
                .map {
                    if (it.duration > 0) it.position.toFloat() / it.duration else 0f
                }
                .collect(fraction)
        }

        launch {
            basePlaybackController.player.progress
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
                .withLatestFrom(basePlaybackController.player.progress) { fraction, progress ->
                    (fraction * progress.duration).toLong()
                }
                .collect {
                    basePlaybackController.player.seekTo(it)
                }
        }
    }

    override fun seekTo(fraction: Float) {
        seekAction.tryEmit(fraction)
    }

    override fun onSeekCommitted(fraction: Float) {
        launch {
            val progress = basePlaybackController.player.progress.first()
            val duration = progress.duration
            if (duration <= 0L) return@launch
            val positionMs = (fraction.coerceIn(0f, 1f) * duration).toLong()
            basePlaybackController.onSeekPositionCommitted(positionMs)
        }
    }

    override fun pause() {
        eventAnalytics.trackEvent(Event.Action.UI.Button.Pause)
        basePlaybackController.pause()
    }

    override fun play() {
        eventAnalytics.trackEvent(Event.Action.UI.Button.Play)
        basePlaybackController.resume()
    }

    override fun next() {
        eventAnalytics.trackEvent(Event.Action.UI.Button.Next)
        basePlaybackController.playNext()
    }

    override fun prev() {
        eventAnalytics.trackEvent(Event.Action.UI.Button.Prev)
        basePlaybackController.playPrev()
    }

    override fun seekBack15Seconds() {
        eventAnalytics.trackEvent(Event.Action.UI.Button.SeekBack15)
        seekBy(-15_000L)
    }

    override fun seekBack1Minute() {
        eventAnalytics.trackEvent(Event.Action.UI.Button.SeekBack60)
        seekBy(-60_000L)
    }

    override fun seekForward15Seconds() {
        eventAnalytics.trackEvent(Event.Action.UI.Button.SeekForward15)
        seekBy(15_000L)
    }

    override fun seekForward1Minute() {
        eventAnalytics.trackEvent(Event.Action.UI.Button.SeekForward60)
        seekBy(60_000L)
    }

    override fun switchMode(isNormal: Boolean) {
        eventAnalytics.trackEvent(Event.Action.UI.Button.ShuffleToggle)
        basePlaybackController.setOrderMode(isNormal)
    }

    private fun seekBy(deltaMs: Long) {
        launch {
            if (basePlaybackController.seekBy(deltaMs)) return@launch
            val progress = basePlaybackController.player.progress.first()
            val duration = progress.duration.coerceAtLeast(0L)
            val target = (progress.position + deltaMs).coerceIn(0L, duration)
            basePlaybackController.player.seekTo(target)
            basePlaybackController.onSeekPositionCommitted(target)
        }
    }

    private companion object {
        private const val FORMAT = "%02d:%02d"
        fun formatTime(time: Long): String =
            abs(time / 1000).let { seconds -> FORMAT.format(seconds / 60, seconds % 60) }.run {
                if (time < 0) "-$this" else this
            }
    }
}
