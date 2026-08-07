package by.tigre.media.platform.entitlements

object FeatureModesRemoteConfig {
    const val RC_FEATURE_MODES = "ff_feature_modes"
    const val RC_UNLOCK_INSTALLATION_IDS = "ff_unlock_installation_ids"

    val remoteConfigDefaults: Map<String, String> = mapOf(
        RC_FEATURE_MODES to "{}",
        RC_UNLOCK_INSTALLATION_IDS to "",
    )

    fun Feature.jsonKey(): String = when (this) {
        Feature.Equalizer -> "equalizer"
        Feature.EqDeviceProfiles -> "eq_device_profiles"
        Feature.SleepTimerAdvanced -> "sleep_timer_advanced"
        Feature.HomeWidget -> "home_widget"
        Feature.ContinueListeningExpanded -> "continue_listening_expanded"
        Feature.UnlimitedPlaylists -> "unlimited_playlists"
    }

    fun featureFromJsonKey(key: String): Feature? =
        Feature.entries.firstOrNull { it.jsonKey() == key }
}
