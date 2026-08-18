package by.tigre.media.platform.entitlements

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class EntitlementsRemoteConfig(
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
) {
    init {
        val defaults = buildMap<String, Any> {
            putAll(EntitlementLimits.remoteConfigDefaults)
            putAll(FeatureModesRemoteConfig.remoteConfigDefaults)
        }
        remoteConfig.setDefaultsAsync(defaults)
    }

    suspend fun refresh() {
        try {
            withTimeoutOrNull(FETCH_TIMEOUT_MILLIS) {
                remoteConfig.fetchAndActivateSuspend()
            }
        } catch (_: Exception) {
            // Hardcoded defaults remain active while Remote Config is unavailable.
        }
    }

    fun featureModes(): Map<Feature, FeatureMode> =
        parseFeatureModes(remoteConfig.getString(FeatureModesRemoteConfig.RC_FEATURE_MODES))

    fun playlistLimit(tier: Tier): Int = when (tier) {
        Tier.Free -> remoteLimit(
            EntitlementLimits.RC_PLAYLIST_LIMIT_FREE,
            EntitlementLimits.defaultPlaylistLimit(tier),
        )

        Tier.Plus -> remoteLimit(
            EntitlementLimits.RC_PLAYLIST_LIMIT_PLUS,
            EntitlementLimits.defaultPlaylistLimit(tier),
        )

        Tier.Pro -> EntitlementLimits.defaultPlaylistLimit(tier)
    }

    fun continueListeningLimit(tier: Tier): Int = when (tier) {
        Tier.Free -> remoteLimit(
            EntitlementLimits.RC_CONTINUE_LISTENING_FREE,
            EntitlementLimits.defaultContinueListeningLimit(tier),
        )

        Tier.Plus -> remoteLimit(
            EntitlementLimits.RC_CONTINUE_LISTENING_PLUS,
            EntitlementLimits.defaultContinueListeningLimit(tier),
        )

        Tier.Pro -> remoteLimit(
            EntitlementLimits.RC_CONTINUE_LISTENING_PRO,
            EntitlementLimits.defaultContinueListeningLimit(tier),
        )
    }

    fun spacesMax(tier: Tier): Int = when (tier) {
        Tier.Free -> remoteLimit(
            EntitlementLimits.RC_SPACES_MAX_FREE,
            EntitlementLimits.defaultSpacesMax(tier),
        )

        Tier.Plus -> remoteLimit(
            EntitlementLimits.RC_SPACES_MAX_PLUS,
            EntitlementLimits.defaultSpacesMax(tier),
        )

        Tier.Pro -> remoteLimit(
            EntitlementLimits.RC_SPACES_MAX_PRO,
            EntitlementLimits.defaultSpacesMax(tier),
        )
    }

    private fun remoteLimit(key: String, default: Int): Int =
        remoteConfig.getLong(key).toInt().takeIf { it > 0 } ?: default

    private suspend fun FirebaseRemoteConfig.fetchAndActivateSuspend(): Boolean =
        suspendCancellableCoroutine { continuation ->
            fetchAndActivate().addOnCompleteListener { task ->
                if (continuation.isActive) {
                    continuation.resume(task.isSuccessful)
                }
            }
        }

    private companion object {
        const val FETCH_TIMEOUT_MILLIS = 10_000L
    }
}
