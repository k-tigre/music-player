package by.tigre.media.platform.inappreview

class InAppReviewController(
    private val store: InAppReviewStore,
    private val playingThresholdMs: Long = DEFAULT_PLAYING_THRESHOLD_MS,
    private val retryAfterMs: Long = DEFAULT_RETRY_AFTER_MS,
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val minLaunches: Int = DEFAULT_MIN_LAUNCHES,
) {
    fun onColdStart() {
        store.incrementLaunchCount()
    }

    fun onPlayingElapsed(deltaMs: Long) {
        store.addPlayingMs(deltaMs, playingThresholdMs)
    }

    fun canRequest(nowMillis: Long, rcEnabled: Boolean): Boolean {
        if (!rcEnabled) return false
        if (store.launchCount() < minLaunches) return false
        if (!store.isQualified()) return false

        val attempts = store.attemptCount()
        if (attempts >= maxAttempts) return false
        if (attempts == 0) return true

        val last = store.lastAttemptAtMillis()
        return last > 0L && nowMillis >= last + retryAfterMs
    }

    fun recordAttempt(nowMillis: Long) {
        store.recordAttempt(nowMillis)
    }

    companion object {
        const val DEFAULT_PLAYING_THRESHOLD_MS = 120_000L
        const val DEFAULT_RETRY_AFTER_MS = 60L * 24 * 60 * 60 * 1000
        const val DEFAULT_MAX_ATTEMPTS = 2
        const val DEFAULT_MIN_LAUNCHES = 2
    }
}
