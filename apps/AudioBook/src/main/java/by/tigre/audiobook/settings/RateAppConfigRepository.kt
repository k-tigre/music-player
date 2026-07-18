package by.tigre.audiobook.settings

import by.tigre.audiobook.BuildConfig
import by.tigre.logger.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class RateAppConfigRepository(
    private val scope: CoroutineScope,
) {
    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    private val _showRateApp = MutableStateFlow(false)
    val showRateApp: StateFlow<Boolean> = _showRateApp.asStateFlow()

    init {
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else REMOTE_FETCH_INTERVAL_SECONDS
            },
        )
        remoteConfig.setDefaultsAsync(RateAppRemoteConfig.defaultsMap())
        publish()
        scope.launch { refresh() }
    }

    fun refresh() {
        scope.launch {
            try {
                remoteConfig.fetchAndActivateSuspend()
                publish()
            } catch (e: Exception) {
                Log.e(e) { "$TAG: Remote config fetch failed" }
            }
        }
    }

    private fun publish() {
        _showRateApp.value = remoteConfig.getBoolean(RateAppRemoteConfig.KEY)
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
        const val TAG = "RateAppConfigRepository"
        const val REMOTE_FETCH_INTERVAL_SECONDS = 3_600L
    }
}
