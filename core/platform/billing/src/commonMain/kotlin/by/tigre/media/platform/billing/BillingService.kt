package by.tigre.media.platform.billing

import kotlinx.coroutines.flow.StateFlow

interface BillingService {
    suspend fun start()

    fun productDetails(productId: String): StateFlow<ProductUi?>

    suspend fun queryProducts(productIds: List<String>)

    suspend fun purchaseSubscription(
        host: BillingPurchaseHost,
        productId: String,
        offerToken: String,
    ): PurchaseResult

    suspend fun purchaseConsumable(
        host: BillingPurchaseHost,
        productId: String,
    ): PurchaseResult

    suspend fun queryActivePurchases(): List<PurchaseSnapshot>

    suspend fun acknowledgeIfNeeded(purchase: PurchaseSnapshot)

    suspend fun consumeTip(purchase: PurchaseSnapshot)
}

sealed class PurchaseResult {
    data object Success : PurchaseResult()

    data object Cancelled : PurchaseResult()

    data class Error(
        val code: Int,
        val message: String,
    ) : PurchaseResult()
}

data class PurchaseSnapshot(
    val productIds: List<String>,
    val purchaseToken: String,
    val isAcknowledged: Boolean,
    val isSubscription: Boolean,
)

data class ProductUi(
    val productId: String,
    val title: String,
    val offers: List<OfferUi>,
)

data class OfferUi(
    val basePlanId: String?,
    val formattedPrice: String,
    val offerToken: String,
)
