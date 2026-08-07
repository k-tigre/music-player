package by.tigre.media.platform.entitlements

import android.content.Context
import by.tigre.media.platform.billing.BillingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayEntitlementsRepository(
    context: Context,
    private val billing: BillingService,
    private val app: AppSku,
    private val cache: EntitlementsCache = EntitlementsCache(context),
    private val remoteConfig: EntitlementsRemoteConfig = EntitlementsRemoteConfig(),
) : EntitlementsRepository {
    private val _tier = MutableStateFlow(cache.load())
    private val _ownedBasePlanIds = MutableStateFlow(cache.loadBasePlanIds())

    override val tier: StateFlow<Tier> = _tier.asStateFlow()
    override val ownedBasePlanIds: StateFlow<Map<String, String>> = _ownedBasePlanIds.asStateFlow()

    override fun has(feature: Feature): Boolean = tier.value.includes(feature.minTier())

    override fun playlistLimit(): Int = remoteConfig.playlistLimit(tier.value)

    override fun continueListeningLimit(): Int = remoteConfig.continueListeningLimit(tier.value)

    override fun rememberSubscriptionBasePlan(productId: String, basePlanId: String) {
        cache.rememberBasePlan(productId, basePlanId)
        _ownedBasePlanIds.value = cache.loadBasePlanIds()
    }

    override suspend fun refresh() {
        try {
            billing.start()
            val purchases = billing.queryActivePurchases()
            purchases
                .asSequence()
                .filter { it.isSubscription && !it.isAcknowledged }
                .forEach { billing.acknowledgeIfNeeded(it) }

            val productIds = purchases.flatMapTo(mutableSetOf()) { it.productIds }
            val resolvedTier = resolveTier(productIds = productIds, app = app)
            _tier.value = resolvedTier
            cache.save(resolvedTier)
            val subscriptionSkus = when (app) {
                AppSku.AudioBook -> SkuIds.AudioBook.subscriptions
                AppSku.Music -> SkuIds.Music.subscriptions
            }.toSet()
            _ownedBasePlanIds.value = cache.retainBasePlansForProducts(
                ownedProductIds = productIds.intersect(subscriptionSkus),
            )
        } catch (_: Exception) {
            // Retain the last known entitlement while Billing is unavailable.
        }
        remoteConfig.refresh()
    }

    override suspend fun restore() = refresh()
}
