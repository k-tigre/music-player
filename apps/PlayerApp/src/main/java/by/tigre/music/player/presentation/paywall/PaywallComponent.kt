package by.tigre.music.player.presentation.paywall

import android.app.Activity
import by.tigre.media.platform.billing.BillingPurchaseHost
import by.tigre.media.platform.billing.BillingService
import by.tigre.media.platform.billing.BillingStoreAvailability
import by.tigre.media.platform.billing.ProductUi
import by.tigre.media.platform.billing.PurchaseResult
import by.tigre.media.platform.entitlements.AppSku
import by.tigre.media.platform.entitlements.EntitlementsRepository
import by.tigre.media.platform.entitlements.SkuIds
import by.tigre.media.platform.entitlements.Tier
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.tools.analytics.common.CommonEventAnalytics
import by.tigre.media.platform.tools.analytics.common.CommonEvents
import by.tigre.music.player.R
import by.tigre.music.player.core.di.PaywallRequest
import by.tigre.music.player.core.di.PaywallSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PaywallThanks {
    Coffee,
    Pizza,
    Subscription,
}

interface PaywallComponent {
    val initialSection: PaywallSection
    val storeAvailability: StateFlow<BillingStoreAvailability>
    val plusProduct: StateFlow<ProductUi?>
    val proProduct: StateFlow<ProductUi?>
    val coffeeTip: StateFlow<ProductUi?>
    val pizzaTip: StateFlow<ProductUi?>
    val tier: StateFlow<Tier>
    val ownedBasePlanIds: StateFlow<Map<String, String>>
    val thanks: StateFlow<PaywallThanks?>

    fun purchaseSubscription(productId: String, offerToken: String, basePlanId: String?)
    fun purchaseTip(productId: String)
    fun restorePurchases()
    fun dismiss()

    class Impl(
        context: BaseComponentContext,
        app: AppSku,
        private val request: PaywallRequest,
        private val activity: Activity,
        private val billing: BillingService,
        private val entitlements: EntitlementsRepository,
        private val eventAnalytics: CommonEventAnalytics,
        private val onTipCompleted: () -> Unit,
        private val onMessage: (Int) -> Unit,
        private val onDismiss: () -> Unit,
    ) : PaywallComponent, BaseComponentContext by context {

        override val initialSection = request.initialSection
        override val storeAvailability = billing.storeAvailability
        override val plusProduct = billing.productDetails(SkuIds.Music.PLUS)
        override val proProduct = billing.productDetails(SkuIds.Music.PRO)
        override val coffeeTip = billing.productDetails(SkuIds.Music.TIP_COFFEE)
        override val pizzaTip = billing.productDetails(SkuIds.Music.TIP_PIZZA)
        override val tier = entitlements.tier
        override val ownedBasePlanIds = entitlements.ownedBasePlanIds

        private val thanksState = MutableStateFlow<PaywallThanks?>(null)
        override val thanks: StateFlow<PaywallThanks?> = thanksState.asStateFlow()

        init {
            check(app == AppSku.Music)
            eventAnalytics.trackEvent(
                CommonEvents.Action.PaywallShown(
                    source = request.source,
                    feature = request.feature.name,
                ),
            )
            launch {
                billing.queryProducts(
                    SkuIds.Music.subscriptions +
                        listOf(SkuIds.Music.TIP_COFFEE, SkuIds.Music.TIP_PIZZA),
                )
            }
        }

        override fun purchaseSubscription(productId: String, offerToken: String, basePlanId: String?) {
            launch {
                eventAnalytics.trackEvent(CommonEvents.Action.PurchaseStarted(productId))
                when (val result = billing.purchaseSubscription(BillingPurchaseHost.from(activity), productId, offerToken)) {
                    PurchaseResult.Success -> {
                        if (!basePlanId.isNullOrBlank()) {
                            entitlements.rememberSubscriptionBasePlan(productId, basePlanId)
                        }
                        entitlements.refresh()
                        eventAnalytics.trackEvent(CommonEvents.Action.PurchaseCompleted(productId))
                        thanksState.value = PaywallThanks.Subscription
                    }
                    PurchaseResult.Cancelled ->
                        eventAnalytics.trackEvent(CommonEvents.Action.PurchaseCancelled(productId))
                    is PurchaseResult.Error -> {
                        eventAnalytics.trackEvent(
                            CommonEvents.Action.PurchaseFailed(productId = productId, code = result.code),
                        )
                        onMessage(R.string.billing_purchase_failed)
                    }
                }
            }
        }

        override fun purchaseTip(productId: String) {
            launch {
                eventAnalytics.trackEvent(CommonEvents.Action.PurchaseStarted(productId))
                when (val result = billing.purchaseConsumable(BillingPurchaseHost.from(activity), productId)) {
                    PurchaseResult.Success -> {
                        onTipCompleted()
                        eventAnalytics.trackEvent(CommonEvents.Action.TipCompleted(productId))
                        thanksState.value = when (productId) {
                            SkuIds.Music.TIP_COFFEE -> PaywallThanks.Coffee
                            SkuIds.Music.TIP_PIZZA -> PaywallThanks.Pizza
                            else -> PaywallThanks.Coffee
                        }
                    }
                    PurchaseResult.Cancelled ->
                        eventAnalytics.trackEvent(CommonEvents.Action.PurchaseCancelled(productId))
                    is PurchaseResult.Error -> {
                        eventAnalytics.trackEvent(
                            CommonEvents.Action.PurchaseFailed(productId = productId, code = result.code),
                        )
                        onMessage(R.string.billing_purchase_failed)
                    }
                }
            }
        }

        override fun restorePurchases() {
            launch {
                runCatching { entitlements.restore() }
                    .onSuccess {
                        eventAnalytics.trackEvent(CommonEvents.Action.PurchaseRestored)
                        onMessage(R.string.billing_restore_complete)
                    }
                    .onFailure { onMessage(R.string.billing_restore_failed) }
            }
        }

        override fun dismiss() {
            eventAnalytics.trackEvent(
                CommonEvents.Action.PaywallDismissed(
                    source = request.source,
                    feature = request.feature.name,
                ),
            )
            onDismiss()
        }
    }
}
