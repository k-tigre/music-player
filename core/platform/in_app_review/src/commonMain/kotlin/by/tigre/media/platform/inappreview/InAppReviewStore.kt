package by.tigre.media.platform.inappreview

import by.tigre.media.platform.preferences.Preferences

class InAppReviewStore(
    private val preferences: Preferences,
) {
    fun launchCount(): Int = preferences.loadInt(KEY_LAUNCH_COUNT, 0)

    fun incrementLaunchCount(): Int {
        val next = launchCount() + 1
        preferences.saveInt(KEY_LAUNCH_COUNT, next)
        return next
    }

    fun playingMs(): Long = preferences.loadLong(KEY_PLAYING_MS, 0L)

    fun isQualified(): Boolean = preferences.loadBoolean(KEY_QUALIFIED, false)

    /**
     * Adds [deltaMs] toward the playback threshold. Returns true once when qualification is first reached.
     */
    fun addPlayingMs(deltaMs: Long, thresholdMs: Long): Boolean {
        if (deltaMs <= 0L || isQualified()) return false
        val next = playingMs() + deltaMs
        preferences.saveLong(KEY_PLAYING_MS, next)
        if (next >= thresholdMs) {
            preferences.saveBoolean(KEY_QUALIFIED, true)
            return true
        }
        return false
    }

    fun attemptCount(): Int = preferences.loadInt(KEY_ATTEMPT_COUNT, 0)

    fun lastAttemptAtMillis(): Long = preferences.loadLong(KEY_LAST_ATTEMPT_AT, 0L)

    fun recordAttempt(nowMillis: Long) {
        preferences.saveInt(KEY_ATTEMPT_COUNT, attemptCount() + 1)
        preferences.saveLong(KEY_LAST_ATTEMPT_AT, nowMillis)
    }

    companion object {
        const val KEY_LAUNCH_COUNT = "in_app_review_launch_count"
        const val KEY_PLAYING_MS = "in_app_review_playing_ms"
        const val KEY_QUALIFIED = "in_app_review_qualified"
        const val KEY_ATTEMPT_COUNT = "in_app_review_attempt_count"
        const val KEY_LAST_ATTEMPT_AT = "in_app_review_last_attempt_at"
    }
}
