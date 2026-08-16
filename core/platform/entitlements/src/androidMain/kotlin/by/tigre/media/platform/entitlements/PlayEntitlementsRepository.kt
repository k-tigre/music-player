package by.tigre.media.platform.entitlements

import android.content.Context
import by.tigre.media.platform.billing.BillingService
import com.google.android.gms.tasks.Tasks
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class PlayEntitlementsRepository(
    context: Context,
    private val billing: BillingService,
    private val app: AppSku,
    private val cache: EntitlementsCache = EntitlementsCache(context),
    private val remoteConfig: EntitlementsRemoteConfig = EntitlementsRemoteConfig(),
    private val installations: FirebaseInstallations = FirebaseInstallations.getInstance(),
) : EntitlementsRepository {
    private val _tier = MutableStateFlow(cache.load())
    private val _ownedBasePlanIds = MutableStateFlow(cache.loadBasePlanIds())

    @Volatile
    private var featureModes: Map<Feature, FeatureMode> = emptyMap()

    @Volatile
    private var unlocked: Boolean = false

    @Volatile
    private var forcePaid: Boolean = false

    override val tier: StateFlow<Tier> = _tier.asStateFlow()
    override val ownedBasePlanIds: StateFlow<Map<String, String>> = _ownedBasePlanIds.asStateFlow()

    override fun has(feature: Feature): Boolean = access(feature) == FeatureAccess.Allowed

    override fun access(feature: Feature): FeatureAccess =
        resolveFeatureAccess(
            feature = feature,
            tier = _tier.value,
            mode = effectiveFeatureMode(
                feature = feature,
                modes = featureModes,
                unlocked = unlocked,
                forcePaid = forcePaid,
            ),
            unlocked = false, // already folded into [effectiveFeatureMode]
        )

    override fun playlistLimit(): Int {
        val mode = effectiveFeatureMode(
            feature = Feature.UnlimitedPlaylists,
            modes = featureModes,
            unlocked = unlocked,
            forcePaid = forcePaid,
        )
        val effectiveTier = limitTier(mode)
        return remoteConfig.playlistLimit(effectiveTier)
    }

    override fun continueListeningLimit(): Int {
        val mode = effectiveFeatureMode(
            feature = Feature.ContinueListeningExpanded,
            modes = featureModes,
            unlocked = unlocked,
            forcePaid = forcePaid,
        )
        val effectiveTier = limitTier(mode)
        return remoteConfig.continueListeningLimit(effectiveTier)
    }

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
        featureModes = remoteConfig.featureModes()
        val installationId = runCatching { fetchInstallationId() }.getOrNull()
        unlocked = installationId != null &&
            installationId in remoteConfig.unlockInstallationIds()
        forcePaid = !unlocked &&
            installationId != null &&
            installationId in remoteConfig.forcePaidInstallationIds()
    }

    override suspend fun restore() = refresh()

    private fun limitTier(mode: FeatureMode): Tier = when {
        unlocked || mode == FeatureMode.On -> Tier.Pro
        mode == FeatureMode.Off -> Tier.Free
        else -> _tier.value
    }

    private suspend fun fetchInstallationId(): String? = withContext(Dispatchers.IO) {
        Tasks.await(installations.id, 5, TimeUnit.SECONDS)
    }
}
