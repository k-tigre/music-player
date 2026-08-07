package by.tigre.media.platform.entitlements

object FeatureModesRemoteConfig {
    const val RC_FEATURE_MODES = "ff_feature_modes"
    const val RC_UNLOCK_INSTALLATION_IDS = "ff_unlock_installation_ids"
    /** CSV of Firebase Installation IDs that see all features as [FeatureMode.Paid]. */
    const val RC_FORCE_PAID_INSTALLATION_IDS = "ff_force_paid_installation_ids"

    /** Explicit free-for-all modes (same as missing keys → on, but clearer in Console). */
    val DEFAULT_FEATURE_MODES_JSON: String =
        """
        {
          "equalizer":"on",
          "eq_device_profiles":"on",
          "sleep_timer_advanced":"on",
          "home_widget":"on",
          "continue_listening_expanded":"on",
          "unlimited_playlists":"on"
        }
        """.trimIndent().replace("\n", "").replace(" ", "")

    /**
     * Placeholder Installation ID — replace in Firebase Console (or here) with a real FID.
     * Devices with this id get paid gating; everyone else gets free (`on`) defaults.
     */
    const val FORCE_PAID_ID_PLACEHOLDER = "XXX"

    val remoteConfigDefaults: Map<String, String> = mapOf(
        RC_FEATURE_MODES to DEFAULT_FEATURE_MODES_JSON,
        RC_UNLOCK_INSTALLATION_IDS to "",
        RC_FORCE_PAID_INSTALLATION_IDS to FORCE_PAID_ID_PLACEHOLDER,
    )

    fun Feature.jsonKey(): String = when (this) {
        Feature.Equalizer -> "equalizer"
        Feature.EqDeviceProfiles -> "eq_device_profiles"
        Feature.SleepTimerAdvanced -> "sleep_timer_advanced"
        Feature.HomeWidget -> "home_widget"
        Feature.ContinueListeningExpanded -> "continue_listening_expanded"
        Feature.UnlimitedPlaylists -> "unlimited_playlists"
        Feature.BookSpaces -> "book_spaces"
    }

    fun featureFromJsonKey(key: String): Feature? =
        Feature.entries.firstOrNull { it.jsonKey() == key }
}
