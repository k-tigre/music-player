package by.tigre.audiobook.presentation.paywall

import android.app.Activity
import by.tigre.audiobook.R
import by.tigre.audiobook.core.di.PaywallRequest
import by.tigre.audiobook.core.di.PaywallSection
import by.tigre.media.platform.billing.BillingPurchaseHost
import by.tigre.media.platform.billing.BillingService
import by.tigre.media.platform.billing.ProductUi
import by.tigre.media.platform.billing.PurchaseResult
import by.tigre.media.platform.entitlements.AppSku
import by.tigre.media.platform.entitlements.EntitlementsRepository
import by.tigre.media.platform.entitlements.SkuIds
import by.tigre.media.platform.entitlements.Tier
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.tools.analytics.book.AudiobookEvents
import by.tigre.media.platform.tools.analytics.book.BookEventAnalytics
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
        private val eventAnalytics: BookEventAnalytics,
        private val onTipCompleted: () -> Unit,
        private val onMessage: (Int) -> Unit,
        private val onDismiss: () -> Unit,
    ) : PaywallComponent, BaseComponentContext by context {

        override val initialSection: PaywallSection = request.initialSection
        override val plusProduct: StateFlow<ProductUi?> = billing.productDetails(SkuIds.AudioBook.PLUS)
        override val proProduct: StateFlow<ProductUi?> = billing.productDetails(SkuIds.AudioBook.PRO)
        override val coffeeTip: StateFlow<ProductUi?> = billing.productDetails(SkuIds.AudioBook.TIP_COFFEE)
        override val pizzaTip: StateFlow<ProductUi?> = billing.productDetails(SkuIds.AudioBook.TIP_PIZZA)
        override val tier = entitlements.tier
        override val ownedBasePlanIds = entitlements.ownedBasePlanIds

        private val thanksState = MutableStateFlow<PaywallThanks?>(null)
        override val thanks: StateFlow<PaywallThanks?> = thanksState.asStateFlow()

        init {
            check(app == AppSku.AudioBook)
            eventAnalytics.trackEvent(AudiobookEvents.Action.PaywallShown(request.source))
            launch {
                billing.queryProducts(
                    SkuIds.AudioBook.subscriptions +
                        listOf(SkuIds.AudioBook.TIP_COFFEE, SkuIds.AudioBook.TIP_PIZZA),
                )
            }
        }

        override fun purchaseSubscription(productId: String, offerToken: String, basePlanId: String?) {
            launch {
                eventAnalytics.trackEvent(AudiobookEvents.Action.PurchaseStarted(productId))
                when (
                    billing.purchaseSubscription(
                        host = BillingPurchaseHost.from(activity),
                        productId = productId,
                        offerToken = offerToken,
                    )
                ) {
                    PurchaseResult.Success -> {
                        if (!basePlanId.isNullOrBlank()) {
                            entitlements.rememberSubscriptionBasePlan(productId, basePlanId)
                        }
                        entitlements.refresh()
                        eventAnalytics.trackEvent(AudiobookEvents.Action.PurchaseCompleted(productId))
                        thanksState.value = PaywallThanks.Subscription
                    }

                    PurchaseResult.Cancelled -> Unit
                    is PurchaseResult.Error -> onMessage(R.string.billing_purchase_failed)
                }
            }
        }

        override fun purchaseTip(productId: String) {
            launch {
                eventAnalytics.trackEvent(AudiobookEvents.Action.PurchaseStarted(productId))
                when (
                    billing.purchaseConsumable(
                        host = BillingPurchaseHost.from(activity),
                        productId = productId,
                    )
                ) {
                    PurchaseResult.Success -> {
                        onTipCompleted()
                        eventAnalytics.trackEvent(AudiobookEvents.Action.TipCompleted(productId))
                        thanksState.value = when (productId) {
                            SkuIds.AudioBook.TIP_COFFEE -> PaywallThanks.Coffee
                            SkuIds.AudioBook.TIP_PIZZA -> PaywallThanks.Pizza
                            else -> PaywallThanks.Coffee
                        }
                    }

                    PurchaseResult.Cancelled -> Unit
                    is PurchaseResult.Error -> onMessage(R.string.billing_purchase_failed)
                }
            }
        }

        override fun restorePurchases() {
            launch {
                runCatching { entitlements.restore() }
                    .onSuccess {
                        eventAnalytics.trackEvent(AudiobookEvents.Action.PurchaseRestored)
                        onMessage(R.string.billing_restore_complete)
                    }
                    .onFailure { onMessage(R.string.billing_restore_failed) }
            }
        }

        override fun dismiss() = onDismiss()
    }
}
