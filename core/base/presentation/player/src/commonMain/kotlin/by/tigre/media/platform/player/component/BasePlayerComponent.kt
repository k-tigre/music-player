package by.tigre.media.platform.player.component

import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.AudioSpectrumSource
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.PlaybackPlayer
import by.tigre.media.platform.playback.prefs.VisualizerPreferences
import by.tigre.media.platform.player.component.BasePlayerComponent.Position
import by.tigre.media.platform.player.component.BasePlayerComponent.State
import by.tigre.media.platform.player.di.PlayerDependency
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.tools.analytics.common.CommonEventAnalytics
import by.tigre.media.platform.tools.analytics.common.CommonEvents
import by.tigre.media.platform.tools.coroutines.extensions.throttleFirst
import by.tigre.media.platform.tools.coroutines.extensions.withLatestFrom
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
    val shuffleEnabled: StateFlow<Boolean>
    val repeatMode: StateFlow<RepeatMode>
    val playbackEqualizer: PlaybackEqualizer
    val audioSpectrumSource: AudioSpectrumSource
    val visualizerPreferences: VisualizerPreferences
    val appPlaybackVolume: AppPlaybackVolume?
    val playbackSpeed: StateFlow<Float>?

    fun pause()
    fun play()
    fun next()
    fun prev()
    fun seekBack15Seconds()
    fun seekBack1Minute()
    fun seekForward15Seconds()
    fun seekForward1Minute()
    fun toggleShuffle()
    fun cycleRepeat()
    fun seekTo(fraction: Float)
    fun onSeekCommitted(fraction: Float) = Unit
    fun returnToQueue() = Unit
    fun setPlaybackSpeed(speed: Float) = Unit
    fun resetPlaybackSpeed() = Unit

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
    private val eventAnalytics: CommonEventAnalytics = dependency.eventAnalytics

    override val playbackEqualizer: PlaybackEqualizer = dependency.playbackEqualizer

    override val audioSpectrumSource: AudioSpectrumSource = dependency.audioSpectrumSource

    override val visualizerPreferences: VisualizerPreferences = dependency.visualizerPreferences

    override val appPlaybackVolume: AppPlaybackVolume? = dependency.appPlaybackVolume

    private val playbackSpeedSource = dependency.playbackSpeedSource
    override val playbackSpeed: StateFlow<Float>? = playbackSpeedSource?.playbackSpeed

    private val seekAction = MutableSharedFlow<Float>(extraBufferCapacity = 1)
    override val currentItem: StateFlow<PlayerItem?> = basePlaybackController.currentItem
        .stateIn(this, SharingStarted.WhileSubscribed(), initialValue = null)
    override val state = MutableStateFlow(State.Paused)
    override val position = MutableStateFlow(Position("", "", ""))
    override val fraction = MutableStateFlow(0f)
    override val shuffleEnabled: StateFlow<Boolean> = basePlaybackController.shuffleEnabled
        .stateIn(this, started = SharingStarted.WhileSubscribed(), initialValue = false)
    override val repeatMode: StateFlow<RepeatMode> = basePlaybackController.repeatMode
        .stateIn(this, started = SharingStarted.WhileSubscribed(), initialValue = RepeatMode.Off)

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
        eventAnalytics.trackEvent(CommonEvents.Action.PlayerPause)
        basePlaybackController.pause()
    }

    override fun play() {
        eventAnalytics.trackEvent(CommonEvents.Action.PlayerPlay)
        basePlaybackController.resume()
    }

    override fun next() {
        eventAnalytics.trackEvent(CommonEvents.Action.PlayerNext)
        basePlaybackController.playNext()
    }

    override fun prev() {
        eventAnalytics.trackEvent(CommonEvents.Action.PlayerPrev)
        basePlaybackController.playPrev()
    }

    override fun seekBack15Seconds() {
        eventAnalytics.trackEvent(CommonEvents.Action.PlayerSeekBack15)
        seekBy(-15_000L)
    }

    override fun seekBack1Minute() {
        eventAnalytics.trackEvent(CommonEvents.Action.PlayerSeekBack60)
        seekBy(-60_000L)
    }

    override fun seekForward15Seconds() {
        eventAnalytics.trackEvent(CommonEvents.Action.PlayerSeekForward15)
        seekBy(15_000L)
    }

    override fun seekForward1Minute() {
        eventAnalytics.trackEvent(CommonEvents.Action.PlayerSeekForward60)
        seekBy(60_000L)
    }

    override fun toggleShuffle() {
        eventAnalytics.trackEvent(CommonEvents.Action.PlayerShuffleToggle)
        basePlaybackController.toggleShuffle()
    }

    override fun cycleRepeat() {
        eventAnalytics.trackEvent(CommonEvents.Action.PlayerRepeatCycle)
        basePlaybackController.cycleRepeat()
    }

    override fun returnToQueue() {
        basePlaybackController.resumeInterruptedSession()
    }

    override fun setPlaybackSpeed(speed: Float) {
        playbackSpeedSource?.setPlaybackSpeed(speed)
    }

    override fun resetPlaybackSpeed() {
        playbackSpeedSource?.resetPlaybackSpeed()
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
