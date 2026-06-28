package by.tigre.audiobook.nighttimer

data class NightTimerShakeConfig(
    val gForceThreshold: Float,
    val debounceMs: Long,
    val pairMaxGapMs: Long,
) {
    fun coerce(): NightTimerShakeConfig = copy(
        gForceThreshold = gForceThreshold.coerceIn(G_FORCE_MIN, G_FORCE_MAX),
        debounceMs = debounceMs.coerceIn(DEBOUNCE_MIN_MS, DEBOUNCE_MAX_MS),
        pairMaxGapMs = pairMaxGapMs.coerceIn(PAIR_GAP_MIN_MS, PAIR_GAP_MAX_MS),
    )

    companion object {
        val Default = NightTimerShakeConfig(
            gForceThreshold = 1.45f,
            debounceMs = 150L,
            pairMaxGapMs = 10_000L,
        )

        const val G_FORCE_MIN = 1.1f
        const val G_FORCE_MAX = 4f
        const val DEBOUNCE_MIN_MS = 100L
        const val DEBOUNCE_MAX_MS = 2_000L
        const val PAIR_GAP_MIN_MS = 3_000L
        const val PAIR_GAP_MAX_MS = 120_000L
    }
}

data class NightTimerShakeDebugState(
    val sensorActive: Boolean = false,
    val detectionEnabled: Boolean = false,
    val testMode: Boolean = false,
    val currentGForce: Float = 1f,
    val shakeCount: Int = 0,
    val firstShakePassed: Boolean = false,
    val pairPassed: Boolean = false,
    val completedPairs: Int = 0,
)
