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

    override val tier: StateFlow<Tier> = _tier.asStateFlow()

    override fun has(feature: Feature): Boolean = tier.value.includes(feature.minTier())

    override fun playlistLimit(): Int = remoteConfig.playlistLimit(tier.value)

    override fun continueListeningLimit(): Int = remoteConfig.continueListeningLimit(tier.value)

    override suspend fun refresh() {
        try {
            billing.start()
            val purchases = billing.queryActivePurchases()
            purchases
                .asSequence()
                .filter { it.isSubscription && !it.isAcknowledged }
                .forEach { billing.acknowledgeIfNeeded(it) }

            val resolvedTier = resolveTier(
                productIds = purchases.flatMapTo(mutableSetOf()) { it.productIds },
                app = app,
            )
            _tier.value = resolvedTier
            cache.save(resolvedTier)
        } catch (_: Exception) {
            // Retain the last known entitlement while Billing is unavailable.
        }
        remoteConfig.refresh()
    }

    override suspend fun restore() = refresh()
}
