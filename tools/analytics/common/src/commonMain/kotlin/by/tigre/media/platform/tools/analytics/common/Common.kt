package by.tigre.media.platform.tools.analytics.common

object CommonEvents {

    sealed class Action(override val name: String) : AnalyticsAction {

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Play / resume playback")
        data object PlayerPlay : Action("common_player_play")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Pause playback")
        data object PlayerPause : Action("common_player_pause")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Skip to next track or chapter")
        data object PlayerNext : Action("common_player_next")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Skip to previous track or chapter")
        data object PlayerPrev : Action("common_player_prev")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Seek backward 15 seconds")
        data object PlayerSeekBack15 : Action("common_player_seek_back_15")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Seek forward 15 seconds")
        data object PlayerSeekForward15 : Action("common_player_seek_forward_15")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Seek backward 60 seconds")
        data object PlayerSeekBack60 : Action("common_player_seek_back_60")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Seek forward 60 seconds")
        data object PlayerSeekForward60 : Action("common_player_seek_forward_60")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Toggle shuffle mode")
        data object PlayerShuffleToggle : Action("common_player_shuffle_toggle")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Cycle repeat mode (off / all / one)")
        data object PlayerRepeatCycle : Action("common_player_repeat_cycle")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Open full player screen")
        data object NavOpenPlayer : Action("common_nav_open_player")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Open equalizer screen")
        data object NavOpenEqualizer : Action("common_nav_open_equalizer")

        @AnalyticsScope(AnalyticsApp.PLAYER)
        @AnalyticsDoc("Open settings screen")
        data object NavOpenSettings : Action("common_nav_open_settings")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Show purchase paywall")
        data class PaywallShown(
            private val source: String,
            private val feature: String,
        ) : Action("common_paywall_shown"), WithPayload {
            override val payload: Map<String, String> = mapOf(
                "source" to source,
                "feature" to feature,
            )
        }

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Dismiss paywall without purchasing")
        data class PaywallDismissed(
            private val source: String,
            private val feature: String,
        ) : Action("common_paywall_dismissed"), WithPayload {
            override val payload: Map<String, String> = mapOf(
                "source" to source,
                "feature" to feature,
            )
        }

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Start purchase or tip billing flow")
        data class PurchaseStarted(private val productId: String) : Action("common_purchase_started"), WithPayload {
            override val payload: Map<String, String> = mapOf("product_id" to productId)
        }

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Complete subscription purchase")
        data class PurchaseCompleted(private val productId: String) : Action("common_purchase_completed"), WithPayload {
            override val payload: Map<String, String> = mapOf("product_id" to productId)
        }

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("User cancelled purchase or tip flow")
        data class PurchaseCancelled(private val productId: String) : Action("common_purchase_cancelled"), WithPayload {
            override val payload: Map<String, String> = mapOf("product_id" to productId)
        }

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Purchase or tip billing flow failed")
        data class PurchaseFailed(
            private val productId: String,
            private val code: Int,
        ) : Action("common_purchase_failed"), WithPayload {
            override val payload: Map<String, String> = mapOf(
                "product_id" to productId,
                "code" to code.toString(),
            )
        }

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Restore previous purchases")
        data object PurchaseRestored : Action("common_purchase_restored")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Complete tip purchase")
        data class TipCompleted(private val productId: String) : Action("common_tip_completed"), WithPayload {
            override val payload: Map<String, String> = mapOf("product_id" to productId)
        }

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Paid or unavailable feature blocked")
        data class FeatureGateBlocked(
            private val feature: String,
            private val reason: String,
            private val source: String = "",
        ) : Action("common_feature_gate_blocked"), WithPayload {
            override val payload: Map<String, String> = buildMap {
                put("feature", feature)
                put("reason", reason)
                if (source.isNotEmpty()) put("source", source)
            }
        }

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("EQ profile saved (manual autosave on leave)")
        data class EqProfileSaved(
            private val target: String,
            private val route: String,
            private val contentKind: String = "",
            private val seededDevice: Boolean = false,
        ) : Action("common_eq_profile_saved"), WithPayload {
            override val payload: Map<String, String> = buildMap {
                put("target", target)
                put("route", route)
                if (contentKind.isNotEmpty()) put("content_kind", contentKind)
                put("seeded_device", seededDevice.toString())
            }
        }

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("EQ profile save failed (limit or missing content)")
        data class EqProfileSaveFailed(
            private val reason: String,
            private val target: String,
        ) : Action("common_eq_profile_save_failed"), WithPayload {
            override val payload: Map<String, String> = mapOf(
                "reason" to reason,
                "target" to target,
            )
        }

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("EQ profile deleted")
        data object EqProfileDeleted : Action("common_eq_profile_deleted")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("User dismissed EQ setup prompt")
        data class EqProfilePromptDismissed(
            private val route: String,
        ) : Action("common_eq_profile_prompt_dismissed"), WithPayload {
            override val payload: Map<String, String> = mapOf("route" to route)
        }

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("EQ profile resolved for current route/content (exact, carry, device_seed, none)")
        data class EqProfileResolved(
            private val outcome: String,
            private val matchLevel: String,
            private val source: String,
            private val routeKind: String,
            private val contentKind: String,
        ) : Action("common_eq_profile_resolved"), WithPayload {
            override val payload: Map<String, String> = mapOf(
                "outcome" to outcome,
                "match_level" to matchLevel,
                "source" to source,
                "route_kind" to routeKind,
                "content_kind" to contentKind,
            )
        }

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("EQ screen closed; dirty+prior auto/carry means user corrected autoset")
        data class EqSessionClosed(
            private val dirty: Boolean,
            private val saved: String,
            private val priorOutcome: String,
            private val priorSource: String,
            private val routeKind: String,
            private val contentKind: String,
            private val matchLevel: String,
        ) : Action("common_eq_session_closed"), WithPayload {
            override val payload: Map<String, String> = mapOf(
                "dirty" to dirty.toString(),
                "saved" to saved,
                "prior_outcome" to priorOutcome,
                "prior_source" to priorSource,
                "route_kind" to routeKind,
                "content_kind" to contentKind,
                "match_level" to matchLevel,
            )
        }

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK)
        @AnalyticsDoc("Subscription tier changed")
        data class SubscriptionTierChanged(
            private val from: String,
            private val to: String,
        ) : Action("common_subscription_tier_changed"), WithPayload {
            override val payload: Map<String, String> = mapOf(
                "from" to from,
                "to" to to,
            )
        }
    }

    sealed class Screen(
        override val name: String,
        override val skip: Boolean = false,
    ) : AnalyticsScreen {

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Full player screen")
        data object Player : Screen("common_screen_player")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.AUDIOBOOK, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Equalizer screen")
        data object Equalizer : Screen("common_screen_equalizer")

        @AnalyticsScope(AnalyticsApp.PLAYER)
        @AnalyticsDoc("Settings screen")
        data object Settings : Screen("common_screen_settings")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Root overlay without a dedicated screen (not sent to analytics)")
        data object RootOverlay : Screen("common_screen_root_overlay", skip = true)
    }
}
