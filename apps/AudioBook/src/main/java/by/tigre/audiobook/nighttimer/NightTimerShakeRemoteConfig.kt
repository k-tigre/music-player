package by.tigre.audiobook.nighttimer

import by.tigre.logger.Log
import org.json.JSONObject

object NightTimerShakeRemoteConfig {
    const val KEY = "audiobook_night_timer_shake_config"

    private const val JSON_G_FORCE = "gForceThreshold"
    private const val JSON_DEBOUNCE_MS = "debounceMs"
    private const val JSON_PAIR_GAP_MS = "pairMaxGapMs"

    fun defaultJson(): String = toJson(NightTimerShakeConfig.Default)

    fun defaultsMap(): Map<String, Any> = mapOf(KEY to defaultJson())

    fun toJson(config: NightTimerShakeConfig): String = JSONObject()
        .put(JSON_G_FORCE, config.gForceThreshold.toDouble())
        .put(JSON_DEBOUNCE_MS, config.debounceMs)
        .put(JSON_PAIR_GAP_MS, config.pairMaxGapMs)
        .toString()

    fun parse(json: String): NightTimerShakeConfig {
        if (json.isBlank()) return NightTimerShakeConfig.Default
        return try {
            val obj = JSONObject(json)
            NightTimerShakeConfig(
                gForceThreshold = obj.optDouble(
                    JSON_G_FORCE,
                    NightTimerShakeConfig.Default.gForceThreshold.toDouble(),
                ).toFloat(),
                debounceMs = obj.optLong(JSON_DEBOUNCE_MS, NightTimerShakeConfig.Default.debounceMs),
                pairMaxGapMs = obj.optLong(JSON_PAIR_GAP_MS, NightTimerShakeConfig.Default.pairMaxGapMs),
            ).coerce()
        } catch (e: Exception) {
            Log.e(e) { "$TAG: Failed to parse shake config JSON, using defaults" }
            NightTimerShakeConfig.Default
        }
    }

    private const val TAG = "NightTimerShakeRemoteConfig"
}

enum class NightTimerShakeConfigSource {
    Remote,
    DebugOverride,
}
