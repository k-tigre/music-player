package by.tigre.media.platform.entitlements

object FeatureModesRemoteConfig {
    const val RC_FEATURE_MODES = "ff_feature_modes"

    /**
     * In-app fallback before the first Remote Config fetch (missing keys also → on).
     * Production values live in Firebase; per-device unlock is a Console/template condition
     * on `app.firebaseInstallationId`, not a client-side ID list.
     */
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

    val remoteConfigDefaults: Map<String, String> = mapOf(
        RC_FEATURE_MODES to DEFAULT_FEATURE_MODES_JSON,
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
