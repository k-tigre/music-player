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

class InAppReviewConfigRepository(
    private val scope: CoroutineScope,
) {
    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    init {
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else REMOTE_FETCH_INTERVAL_SECONDS
            },
        )
        remoteConfig.setDefaultsAsync(
            RateAppRemoteConfig.defaultsMap() + InAppReviewRemoteConfig.defaultsMap(),
        )
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

    fun isEnabled(): Boolean = _enabled.value

    private fun publish() {
        _enabled.value = remoteConfig.getBoolean(InAppReviewRemoteConfig.KEY)
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
        const val TAG = "InAppReviewConfigRepository"
        const val REMOTE_FETCH_INTERVAL_SECONDS = 3_600L
    }
}
