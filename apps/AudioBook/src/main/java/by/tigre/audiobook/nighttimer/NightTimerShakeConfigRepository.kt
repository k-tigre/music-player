package by.tigre.audiobook.nighttimer

import by.tigre.audiobook.BuildConfig
import by.tigre.logger.Log
import by.tigre.media.platform.preferences.Preferences
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class NightTimerShakeConfigRepository(
    private val preferences: Preferences,
    private val scope: CoroutineScope,
) {
    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    private val _config = MutableStateFlow(readEffectiveConfig())
    val config: StateFlow<NightTimerShakeConfig> = _config.asStateFlow()

    private val _source = MutableStateFlow(computeSource())
    val source: StateFlow<NightTimerShakeConfigSource> = _source.asStateFlow()

    private val _fetching = MutableStateFlow(false)
    val fetching: StateFlow<Boolean> = _fetching.asStateFlow()

    init {
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else REMOTE_FETCH_INTERVAL_SECONDS
            },
        )
        remoteConfig.setDefaultsAsync(NightTimerShakeRemoteConfig.defaultsMap())
        publishEffectiveConfig()
        scope.launch { refresh() }
    }

    fun refresh() {
        scope.launch {
            if (_fetching.value) return@launch
            _fetching.value = true
            try {
                val updated = remoteConfig.fetchAndActivateSuspend()
                publishEffectiveConfig()
                Log.d(TAG) { "Remote config fetch: updated=$updated, config=${_config.value}" }
            } catch (e: Exception) {
                Log.e(e) { "$TAG: Remote config fetch failed" }
            } finally {
                _fetching.value = false
            }
        }
    }

    fun update(config: NightTimerShakeConfig) {
        if (!BuildConfig.DEBUG) return
        val coerced = config.coerce()
        saveDebugOverride(coerced)
        publishEffectiveConfig()
    }

    fun resetToDefaults() {
        if (BuildConfig.DEBUG) {
            clearDebugOverride()
        }
        publishEffectiveConfig()
    }

    private fun publishEffectiveConfig() {
        _config.value = readEffectiveConfig()
        _source.value = computeSource()
    }

    private fun readEffectiveConfig(): NightTimerShakeConfig {
        return readDebugOverride() ?: readRemoteConfig()
    }

    private fun computeSource(): NightTimerShakeConfigSource {
        return if (BuildConfig.DEBUG && readDebugOverride() != null) {
            NightTimerShakeConfigSource.DebugOverride
        } else {
            NightTimerShakeConfigSource.Remote
        }
    }

    private fun readRemoteConfig(): NightTimerShakeConfig {
        return NightTimerShakeRemoteConfig.parse(
            remoteConfig.getString(NightTimerShakeRemoteConfig.KEY),
        )
    }

    private fun readDebugOverride(): NightTimerShakeConfig? {
        if (!BuildConfig.DEBUG) return null
        if (!preferences.loadBoolean(PREF_DEBUG_OVERRIDE, false)) return null
        return NightTimerShakeRemoteConfig.parse(
            preferences.loadString(PREF_DEBUG_JSON, null).orEmpty(),
        )
    }

    private fun saveDebugOverride(config: NightTimerShakeConfig) {
        preferences.saveBoolean(PREF_DEBUG_OVERRIDE, true)
        preferences.saveString(PREF_DEBUG_JSON, NightTimerShakeRemoteConfig.toJson(config.coerce()))
    }

    private fun clearDebugOverride() {
        preferences.saveBoolean(PREF_DEBUG_OVERRIDE, false)
    }

    private suspend fun FirebaseRemoteConfig.fetchAndActivateSuspend(): Boolean =
        suspendCancellableCoroutine { continuation ->
            fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (continuation.isActive) {
                        continuation.resume(task.isSuccessful)
                    }
                }
        }

    private companion object {
        const val TAG = "NightTimerShakeConfig"
        const val REMOTE_FETCH_INTERVAL_SECONDS = 3_600L

        const val PREF_DEBUG_OVERRIDE = "night_timer_shake_debug_override"
        const val PREF_DEBUG_JSON = "night_timer_shake_debug_json"
    }
}
