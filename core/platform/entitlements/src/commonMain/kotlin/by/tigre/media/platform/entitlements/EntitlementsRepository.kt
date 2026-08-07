package by.tigre.media.platform.entitlements

import kotlinx.coroutines.flow.StateFlow

interface EntitlementsRepository {
    val tier: StateFlow<Tier>

    /** productId → basePlanId for active subscriptions (empty if unknown / free). */
    val ownedBasePlanIds: StateFlow<Map<String, String>>

    fun has(feature: Feature): Boolean

    fun access(feature: Feature): FeatureAccess

    fun playlistLimit(): Int

    fun continueListeningLimit(): Int

    fun rememberSubscriptionBasePlan(productId: String, basePlanId: String)

    suspend fun refresh()

    suspend fun restore()
}
