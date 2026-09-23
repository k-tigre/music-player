package by.tigre.media.platform.inappreview

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Accumulates Playing wall-clock time and launches In-App Review at most once per process
 * when [controller] is eligible and [isRcEnabled] is true.
 */
class InAppReviewPrompt(
    private val controller: InAppReviewController,
    private val launcher: InAppReviewLauncher,
    private val scope: CoroutineScope,
    private val isRcEnabled: () -> Boolean,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val onRequested: () -> Unit = {},
) {
    private val mutex = Mutex()
    private var attemptedThisProcess = false
    private var accumulationJob: Job? = null

    fun onColdStart() {
        controller.onColdStart()
    }

    fun bindIsPlaying(isPlaying: Flow<Boolean>) {
        accumulationJob?.cancel()
        accumulationJob = scope.launch {
            isPlaying.distinctUntilChanged().collectLatest { playing ->
                if (!playing) return@collectLatest
                var lastTickAt = clockMillis()
                while (isActive) {
                    delay(TICK_MS)
                    val now = clockMillis()
                    controller.onPlayingElapsed(now - lastTickAt)
                    lastTickAt = now
                }
            }
        }
    }

    fun maybeLaunch(activity: ActivityRef) {
        scope.launch {
            mutex.withLock {
                if (attemptedThisProcess) return@withLock
                val now = clockMillis()
                if (!controller.canRequest(now, isRcEnabled())) return@withLock
                attemptedThisProcess = true
                val started = try {
                    launcher.launch(activity)
                } catch (_: Exception) {
                    false
                }
                if (started) {
                    controller.recordAttempt(now)
                    onRequested()
                } else {
                    // Allow another try later in this process if request failed before launch.
                    attemptedThisProcess = false
                }
            }
        }
    }

    private companion object {
        const val TICK_MS = 1_000L
    }
}
