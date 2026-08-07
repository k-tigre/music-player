package by.tigre.media.platform.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object NoOpBillingService : BillingService {
    private val emptyProduct = MutableStateFlow<ProductUi?>(null)

    override suspend fun start() = Unit

    override fun productDetails(productId: String): StateFlow<ProductUi?> = emptyProduct

    override suspend fun queryProducts(productIds: List<String>) = Unit

    override suspend fun purchaseSubscription(
        host: BillingPurchaseHost,
        productId: String,
        offerToken: String,
    ): PurchaseResult = PurchaseResult.Cancelled

    override suspend fun purchaseConsumable(
        host: BillingPurchaseHost,
        productId: String,
    ): PurchaseResult = PurchaseResult.Cancelled

    override suspend fun queryActivePurchases(): List<PurchaseSnapshot> = emptyList()

    override suspend fun acknowledgeIfNeeded(purchase: PurchaseSnapshot) = Unit

    override suspend fun consumeTip(purchase: PurchaseSnapshot) = Unit
}
