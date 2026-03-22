package by.tigre.audiobook.core.data.audiobook_playback

import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackConfig.RESUME_REWIND_RAMP_MS


internal object AudiobookPlaybackConfig {
    const val BOOK_COMPLETION_THRESHOLD_MS = 10_000L // 10 seconds

    /** Rewind after resume when pause duration is known (linear ramp up to [RESUME_REWIND_RAMP_MS]). */
    const val RESUME_REWIND_MIN_MS = 2_000L
    const val RESUME_REWIND_MAX_MS = 10_000L
    const val RESUME_REWIND_RAMP_MS = 600_000L // 10 minutes — at this (or longer) pause, use max rewind

    /** When pause was tracked but duration is unavailable (e.g. future: restore after process death without timestamp). */
    const val RESUME_REWIND_WHEN_PAUSE_UNKNOWN_MS = 10_000L
}
