package by.tigre.audiobook.nighttimer

import android.content.Context
import android.os.Looper
import android.os.SystemClock
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.nighttimer.NightTimerControllerImpl.Companion.FACE_DOWN_GATE_SECONDS
import by.tigre.music.player.core.data.playback.AppPlaybackVolume
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.data.storage.preferences.Preferences
import by.tigre.music.player.logger.Log
import by.tigre.music.player.tools.coroutines.CoreScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

data class NightTimerUiState(
    val isRunning: Boolean,
    val remainingSeconds: Int,
)

interface NightTimerController {

    val uiState: StateFlow<NightTimerUiState>

    val selectedMinutes: StateFlow<Int>
    val fadeOutAtEnd: StateFlow<Boolean>

    fun setSelectedMinutes(minutes: Int)
    fun setFadeOutAtEnd(enabled: Boolean)

    fun startTimer()
    fun cancelTimer()
}

fun createNightTimerController(
    context: Context,
    preferences: Preferences,
    playbackController: AudiobookPlaybackController,
    appPlaybackVolume: AppPlaybackVolume,
    scope: CoreScope,
): NightTimerController = NightTimerControllerImpl(
    context,
    preferences,
    playbackController,
    appPlaybackVolume,
    scope,
)

private class NightTimerControllerImpl(
    context: Context,
    private val preferences: Preferences,
    private val playbackController: AudiobookPlaybackController,
    private val appPlaybackVolume: AppPlaybackVolume,
    private val scope: CoreScope,
) : NightTimerController {

    private val faceDownExtender = FaceDownFlipExtender(context, ::extendByFaceDown)

    private val _uiState = MutableStateFlow(NightTimerUiState(isRunning = false, remainingSeconds = 0))
    override val uiState: StateFlow<NightTimerUiState> = _uiState.asStateFlow()

    private val _selectedMinutes = MutableStateFlow(loadMinutes())
    override val selectedMinutes: StateFlow<Int> = _selectedMinutes.asStateFlow()

    private val _fadeOutAtEnd = MutableStateFlow(loadFade())
    override val fadeOutAtEnd: StateFlow<Boolean> = _fadeOutAtEnd.asStateFlow()

    private var timerJob: Job? = null
    private val endAtElapsedRealtime = AtomicLong(0L)

    private var volumeBeforeTimer: Float = 1f
    private var fadeBaseVolume: Float = 1f
    private var fadeBaseCaptured: Boolean = false

    /** Becomes true once remaining drops below [FACE_DOWN_GATE_SECONDS]; used to reset the flip detector once. */
    private var faceDownGateEntered: Boolean = false

    override fun setSelectedMinutes(minutes: Int) {
        if (minutes !in ALLOWED_MINUTES_SET) return
        _selectedMinutes.value = minutes
        preferences.saveInt(PREF_MINUTES, minutes)
    }

    override fun setFadeOutAtEnd(enabled: Boolean) {
        _fadeOutAtEnd.value = enabled
        preferences.saveBoolean(PREF_FADE, enabled)
    }

    override fun startTimer() {
        cancelTimerInternal(restoreVolume = true)
        val minutes = _selectedMinutes.value
        val fade = _fadeOutAtEnd.value
        volumeBeforeTimer = appPlaybackVolume.playbackVolume.value
        fadeBaseCaptured = false
        faceDownGateEntered = false
        endAtElapsedRealtime.set(SystemClock.elapsedRealtime() + minutes * 60_000L)
        faceDownExtender.start()
        timerJob = scope.launch {
            try {
                runTimerLoop(fade)
            } finally {
                faceDownExtender.stop()
            }
        }
        publishRemaining()
    }

    override fun cancelTimer() {
        cancelTimerInternal(restoreVolume = true)
    }

    private fun cancelTimerInternal(restoreVolume: Boolean) {
        timerJob?.cancel()
        timerJob = null
        faceDownExtender.stop()
        if (restoreVolume) {
            appPlaybackVolume.setPlaybackVolume(volumeBeforeTimer)
        }
        _uiState.value = NightTimerUiState(isRunning = false, remainingSeconds = 0)
    }

    private suspend fun runTimerLoop(fadeEnabled: Boolean) {
        while (true) {
            val remaining = computeRemainingSeconds()
            _uiState.value = NightTimerUiState(isRunning = true, remainingSeconds = remaining)

            if (remaining < FACE_DOWN_GATE_SECONDS) {
                if (!faceDownGateEntered) {
                    faceDownGateEntered = true
                    faceDownExtender.resetDetectionState()
                }
            } else {
                faceDownGateEntered = false
            }

            if (remaining <= 0) {
                onTimerFinished(fadeEnabled)
                return
            }

            if (fadeEnabled && remaining > FADE_WINDOW_SECONDS && fadeBaseCaptured) {
                setPlaybackVolumeOnMainSuspend(volumeBeforeTimer)
                fadeBaseCaptured = false
            }

            if (fadeEnabled && remaining <= FADE_WINDOW_SECONDS) {
                if (!fadeBaseCaptured) {
                    fadeBaseVolume = appPlaybackVolume.playbackVolume.value
                    fadeBaseCaptured = true
                }
                applyFadeVolumeOnMain(remaining)
            }

            delay(TICK_MS)
        }
    }

    private fun computeRemainingSeconds(): Int {
        val left = ((endAtElapsedRealtime.get() - SystemClock.elapsedRealtime()) / 1000L).toInt()
        return left.coerceAtLeast(0)
    }

    private fun publishRemaining() {
        val remaining = computeRemainingSeconds()
        _uiState.value = NightTimerUiState(isRunning = true, remainingSeconds = remaining)
    }

    private fun extendByFaceDown() {
        if (timerJob?.isActive != true) return
        if (computeRemainingSeconds() >= FACE_DOWN_GATE_SECONDS) return
        endAtElapsedRealtime.addAndGet(FACE_DOWN_EXTRA_MS)
        Log.d(TAG) { "Face down: +5 min" }
        val remaining = computeRemainingSeconds()
        if (remaining > FADE_WINDOW_SECONDS && fadeBaseCaptured) {
            setPlaybackVolumeOnMainSync(volumeBeforeTimer)
            fadeBaseCaptured = false
        }
        _uiState.value = NightTimerUiState(isRunning = true, remainingSeconds = remaining)
    }

    private suspend fun applyFadeVolumeOnMain(remainingSeconds: Int) {
        val elapsed = FADE_WINDOW_SECONDS - remainingSeconds
        val step = (elapsed / FADE_STEP_SECONDS).coerceIn(0, FADE_STEPS)
        val factor = (FADE_STEPS - step) / FADE_STEPS.toFloat()
        setPlaybackVolumeOnMainSuspend(fadeBaseVolume * factor)
    }

    private suspend fun setPlaybackVolumeOnMainSuspend(volume: Float) {
        withContext(Dispatchers.Main.immediate) {
            appPlaybackVolume.setPlaybackVolume(volume)
        }
    }

    private fun setPlaybackVolumeOnMainSync(volume: Float) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            appPlaybackVolume.setPlaybackVolume(volume)
        } else {
            runBlocking(Dispatchers.Main.immediate) {
                appPlaybackVolume.setPlaybackVolume(volume)
            }
        }
    }

    private suspend fun onTimerFinished(fadeEnabled: Boolean) {
        faceDownExtender.stop()
        timerJob = null
        withContext(Dispatchers.Main.immediate) {
            val wasPlaying = playbackController.player.state.value == PlaybackPlayer.State.Playing
            val rewindMs =
                if (fadeEnabled && wasPlaying) NIGHT_TIMER_REWIND_MS else null
            appPlaybackVolume.setPlaybackVolume(volumeBeforeTimer)
            playbackController.endPlaybackForNightTimer(rewindMs)
            _uiState.value = NightTimerUiState(isRunning = false, remainingSeconds = 0)
            Log.d(TAG) { "Night timer finished, wasPlaying=$wasPlaying, rewindMs=$rewindMs" }
        }
    }

    private fun loadMinutes(): Int {
        val v = preferences.loadInt(PREF_MINUTES, DEFAULT_MINUTES)
        return if (v in ALLOWED_MINUTES_SET) v else DEFAULT_MINUTES
    }

    private fun loadFade(): Boolean = preferences.loadBoolean(PREF_FADE, DEFAULT_FADE)

    private companion object {
        const val TAG = "NightTimer"
        const val PREF_MINUTES = "night_timer_minutes"
        const val PREF_FADE = "night_timer_fade_out"
        const val DEFAULT_MINUTES = 15
        const val DEFAULT_FADE = false
        val ALLOWED_MINUTES_SET = setOf(5, 10, 15, 20, 30)
        const val TICK_MS = 1_000L
        const val FADE_WINDOW_SECONDS = 60
        const val FADE_STEP_SECONDS = 6
        const val FADE_STEPS = 10
        const val FACE_DOWN_EXTRA_MS = 5 * 60_000L

        /** Face-down adds time only while remaining is strictly below this many seconds (less than one minute). */
        const val FACE_DOWN_GATE_SECONDS = 60
        const val NIGHT_TIMER_REWIND_MS = 30_000L
    }
}
