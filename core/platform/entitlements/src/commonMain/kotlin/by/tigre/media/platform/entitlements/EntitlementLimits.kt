package by.tigre.media.platform.entitlements

object EntitlementLimits {
    const val RC_PLAYLIST_LIMIT_FREE = "entitlements_playlist_limit_free"
    const val RC_PLAYLIST_LIMIT_PLUS = "entitlements_playlist_limit_plus"

    const val RC_CONTINUE_LISTENING_FREE = "entitlements_continue_listening_free"
    const val RC_CONTINUE_LISTENING_PLUS = "entitlements_continue_listening_plus"
    const val RC_CONTINUE_LISTENING_PRO = "entitlements_continue_listening_pro"

    const val RC_SPACES_MAX_FREE = "ff_book_spaces_max_free"
    const val RC_SPACES_MAX_PLUS = "ff_book_spaces_max_plus"
    const val RC_SPACES_MAX_PRO = "ff_book_spaces_max_pro"

    val remoteConfigDefaults: Map<String, Int> = mapOf(
        RC_PLAYLIST_LIMIT_FREE to 3,
        RC_PLAYLIST_LIMIT_PLUS to 10,
        RC_CONTINUE_LISTENING_FREE to 3,
        RC_CONTINUE_LISTENING_PLUS to 5,
        RC_CONTINUE_LISTENING_PRO to 10,
        RC_SPACES_MAX_FREE to 1,
        RC_SPACES_MAX_PLUS to 1,
        RC_SPACES_MAX_PRO to 5,
    )

    fun defaultPlaylistLimit(tier: Tier): Int = when (tier) {
        Tier.Free -> 3
        Tier.Plus -> 10
        Tier.Pro -> Int.MAX_VALUE
    }

    fun defaultContinueListeningLimit(tier: Tier): Int = when (tier) {
        Tier.Free -> 3
        Tier.Plus -> 5
        Tier.Pro -> 10
    }

    fun defaultSpacesMax(tier: Tier): Int = when (tier) {
        Tier.Free -> 1
        Tier.Plus -> 1
        Tier.Pro -> 5
    }
}
