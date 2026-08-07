package by.tigre.media.platform.entitlements

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FreeEntitlementsRepository : EntitlementsRepository {
    override val tier: StateFlow<Tier> = MutableStateFlow(Tier.Free)
    override val ownedBasePlanIds: StateFlow<Map<String, String>> = MutableStateFlow(emptyMap())

    override fun has(feature: Feature): Boolean = access(feature) == FeatureAccess.Allowed

    override fun access(feature: Feature): FeatureAccess =
        // Desktop has no billing; treat all features as available (transition default = on).
        FeatureAccess.Allowed

    override fun playlistLimit(): Int = EntitlementLimits.defaultPlaylistLimit(Tier.Pro)

    override fun continueListeningLimit(): Int =
        EntitlementLimits.defaultContinueListeningLimit(Tier.Pro)

    override fun rememberSubscriptionBasePlan(productId: String, basePlanId: String) = Unit

    override suspend fun refresh() = Unit

    override suspend fun restore() = Unit
}
