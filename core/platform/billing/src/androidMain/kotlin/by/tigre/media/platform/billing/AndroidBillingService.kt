package by.tigre.media.platform.billing

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

class AndroidBillingService(
    context: Context,
) : BillingService {
    private val appContext = context.applicationContext
    private val productFlows = ConcurrentHashMap<String, MutableStateFlow<ProductUi?>>()
    private val cachedProducts = ConcurrentHashMap<String, CachedProduct>()
    private val connectionMutex = Mutex()
    private val purchaseMutex = Mutex()
    private val _storeAvailability = MutableStateFlow(BillingStoreAvailability.Unknown)

    @Volatile
    private var connection: CompletableDeferred<BillingResult>? = null

    @Volatile
    private var pendingPurchase: CompletableDeferred<PurchaseResult>? = null

    @Volatile
    private var completedPurchase: Purchase? = null

    private val client = BillingClient.newBuilder(appContext)
        .setListener(PurchasesUpdatedListener(::onPurchasesUpdated))
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .enableAutoServiceReconnection()
        .build()

    override val storeAvailability: StateFlow<BillingStoreAvailability> = _storeAvailability

    override suspend fun start() {
        ensureStarted()
    }

    override fun productDetails(productId: String): StateFlow<ProductUi?> =
        productFlows.getOrPut(productId) { MutableStateFlow(null) }

    override suspend fun queryProducts(productIds: List<String>) {
        if (ensureStarted().responseCode != BillingClient.BillingResponseCode.OK) return

        queryProductType(productIds, BillingClient.ProductType.SUBS)
        queryProductType(productIds, BillingClient.ProductType.INAPP)
    }

    override suspend fun purchaseSubscription(
        host: BillingPurchaseHost,
        productId: String,
        offerToken: String,
    ): PurchaseResult {
        val existingSubscription = findExistingSubscriptionForUpdate(productId)
        return launchPurchase(
            host = host,
            product = cachedProducts[productId],
            offerToken = offerToken,
            expectedType = BillingClient.ProductType.SUBS,
            subscriptionUpdate = existingSubscription,
        )
    }

    override suspend fun purchaseConsumable(
        host: BillingPurchaseHost,
        productId: String,
    ): PurchaseResult {
        val product = cachedProducts[productId]
        val result = launchPurchase(
            host = host,
            product = product,
            offerToken = product?.details?.oneTimePurchaseOfferDetailsList
                ?.firstOrNull()
                ?.offerToken,
            expectedType = BillingClient.ProductType.INAPP,
            subscriptionUpdate = null,
        )
        if (result is PurchaseResult.Success) {
            val purchase = completedPurchase
                ?.takeIf { productId in it.products }
                ?.toSnapshot(isSubscription = false)
            if (purchase != null) {
                consumeTip(purchase)
            }
        }
        return result
    }

    override suspend fun queryActivePurchases(): List<PurchaseSnapshot> {
        val connectionResult = ensureStarted()
        if (connectionResult.responseCode != BillingClient.BillingResponseCode.OK) {
            error(
                "Billing unavailable: code=${connectionResult.responseCode} " +
                    "message=${connectionResult.debugMessage}",
            )
        }

        return queryPurchases(BillingClient.ProductType.SUBS).map { it.toSnapshot(isSubscription = true) } +
            queryPurchases(BillingClient.ProductType.INAPP).map { it.toSnapshot(isSubscription = false) }
    }

    override suspend fun acknowledgeIfNeeded(purchase: PurchaseSnapshot) {
        if (purchase.isAcknowledged) return

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val deferred = CompletableDeferred<Unit>()
        client.acknowledgePurchase(params) {
            deferred.complete(Unit)
        }
        deferred.await()
    }

    override suspend fun consumeTip(purchase: PurchaseSnapshot) {
        if (purchase.isSubscription) return

        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val deferred = CompletableDeferred<Unit>()
        client.consumeAsync(params) { _, _ ->
            deferred.complete(Unit)
        }
        deferred.await()
    }

    private suspend fun launchPurchase(
        host: BillingPurchaseHost,
        product: CachedProduct?,
        offerToken: String?,
        expectedType: String,
        subscriptionUpdate: SubscriptionUpdateTarget?,
    ): PurchaseResult = purchaseMutex.withLock {
        val connectionResult = ensureStarted()
        if (connectionResult.responseCode != BillingClient.BillingResponseCode.OK || !client.isReady) {
            return@withLock connectionResult.toPurchaseError()
        }
        val activity = host.activity
        if (activity.isFinishing || activity.isDestroyed) {
            return@withLock PurchaseResult.Error(
                code = BillingClient.BillingResponseCode.ERROR,
                message = "Host activity is not available for billing flow.",
            )
        }
        if (product == null || product.type != expectedType) {
            return@withLock PurchaseResult.Error(
                code = BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                message = "Product details are unavailable. Call queryProducts before purchasing.",
            )
        }

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product.details)
            .apply { offerToken?.let(::setOfferToken) }
        if (subscriptionUpdate != null) {
            productParamsBuilder.setSubscriptionProductReplacementParams(
                BillingFlowParams.ProductDetailsParams.SubscriptionProductReplacementParams
                    .newBuilder()
                    .setOldProductId(subscriptionUpdate.oldProductId)
                    .setReplacementMode(
                        BillingFlowParams.ProductDetailsParams.SubscriptionProductReplacementParams
                            .ReplacementMode.CHARGE_FULL_PRICE,
                    )
                    .build(),
            )
        }
        val paramsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
        if (subscriptionUpdate != null) {
            paramsBuilder.setSubscriptionUpdateParams(
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(subscriptionUpdate.oldPurchaseToken)
                    .build(),
            )
        }
        val params = paramsBuilder.build()
        val deferred = CompletableDeferred<PurchaseResult>()
        completedPurchase = null
        pendingPurchase = deferred
        try {
            val launchResult = runCatching {
                client.launchBillingFlow(activity, params)
            }.getOrElse { error ->
                return@withLock PurchaseResult.Error(
                    code = BillingClient.BillingResponseCode.ERROR,
                    message = error.message ?: "launchBillingFlow failed",
                )
            }
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                deferred.complete(launchResult.toPurchaseResult())
            }
            deferred.await()
        } finally {
            pendingPurchase = null
        }
    }

    /**
     * Base-plan change (monthly→yearly) and Plus→Pro both need the old purchase token.
     * Prefer a purchase that already contains [newProductId]; otherwise any active subscription.
     */
    private suspend fun findExistingSubscriptionForUpdate(
        newProductId: String,
    ): SubscriptionUpdateTarget? {
        if (ensureStarted().responseCode != BillingClient.BillingResponseCode.OK) return null
        val purchases = queryPurchases(BillingClient.ProductType.SUBS)
        val sameProduct = purchases.firstOrNull { newProductId in it.products }
        if (sameProduct != null) {
            return SubscriptionUpdateTarget(
                oldPurchaseToken = sameProduct.purchaseToken,
                oldProductId = newProductId,
            )
        }
        val other = purchases.firstOrNull() ?: return null
        val oldProductId = other.products.firstOrNull() ?: return null
        return SubscriptionUpdateTarget(
            oldPurchaseToken = other.purchaseToken,
            oldProductId = oldProductId,
        )
    }

    private suspend fun ensureStarted(): BillingResult {
        if (client.isReady) {
            markAvailability(available = true)
            return readyResult
        }

        var lastResult = disconnectedResult
        repeat(MAX_CONNECTION_ATTEMPTS) {
            val deferred = connectionMutex.withLock {
                if (client.isReady) {
                    markAvailability(available = true)
                    return readyResult
                }
                connection ?: CompletableDeferred<BillingResult>().also { created ->
                    connection = created
                    client.startConnection(
                        object : BillingClientStateListener {
                            override fun onBillingSetupFinished(billingResult: BillingResult) {
                                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                                    connection = null
                                }
                                created.complete(billingResult)
                            }

                            override fun onBillingServiceDisconnected() {
                                connection = null
                                markAvailability(available = false)
                            }
                        },
                    )
                }
            }
            val result = deferred.await()
            lastResult = result
            if (
                result.responseCode == BillingClient.BillingResponseCode.OK &&
                client.isReady
            ) {
                markAvailability(available = true)
                return result
            }
            connectionMutex.withLock {
                if (connection === deferred) {
                    connection = null
                }
            }
        }
        markAvailability(available = false)
        return lastResult
    }

    private fun markAvailability(available: Boolean) {
        _storeAvailability.value = if (available) {
            BillingStoreAvailability.Available
        } else {
            BillingStoreAvailability.Unavailable
        }
    }

    private suspend fun queryProductType(
        productIds: List<String>,
        type: String,
    ) {
        if (productIds.isEmpty()) return

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(type)
                        .build()
                },
            )
            .build()
        val deferred = CompletableDeferred<Pair<BillingResult, List<ProductDetails>>>()
        client.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            deferred.complete(billingResult to productDetailsResult.productDetailsList)
        }
        val result = deferred.await()
        if (result.first.responseCode != BillingClient.BillingResponseCode.OK) return

        result.second.forEach { details ->
            cachedProducts[details.productId] = CachedProduct(details, type)
            productFlows.getOrPut(details.productId) { MutableStateFlow(null) }.value =
                details.toProductUi()
        }
    }

    private suspend fun queryPurchases(type: String): List<Purchase> {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(type)
            .build()
        // Play Billing may invoke the listener twice (e.g. watchdog timeout + real
        // response). CompletableDeferred.complete() keeps only the first result.
        val deferred = CompletableDeferred<List<Purchase>>()
        client.queryPurchasesAsync(params) { billingResult, purchases ->
            deferred.complete(
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases
                } else {
                    emptyList()
                },
            )
        }
        return deferred.await()
    }

    private fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) {
        val pending = pendingPurchase ?: return
        completedPurchase = purchases?.firstOrNull()
        val result = when {
            billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty() ->
                PurchaseResult.Success

            else -> billingResult.toPurchaseResult()
        }
        pending.complete(result)
    }

    private fun ProductDetails.toProductUi(): ProductUi {
        val subscriptionOffers = subscriptionOfferDetails.orEmpty().map {
            OfferUi(
                basePlanId = it.basePlanId,
                formattedPrice = it.pricingPhases.pricingPhaseList.firstOrNull()?.formattedPrice.orEmpty(),
                offerToken = it.offerToken,
            )
        }
        val oneTimeOffers = oneTimePurchaseOfferDetailsList.orEmpty().mapNotNull { offer ->
            offer.offerToken?.let { token ->
                OfferUi(
                    basePlanId = null,
                    formattedPrice = offer.formattedPrice.orEmpty(),
                    offerToken = token,
                )
            }
        }
        return ProductUi(
            productId = productId,
            title = title,
            offers = subscriptionOffers + oneTimeOffers,
        )
    }

    private fun Purchase.toSnapshot(isSubscription: Boolean): PurchaseSnapshot = PurchaseSnapshot(
        productIds = products,
        purchaseToken = purchaseToken,
        isAcknowledged = isAcknowledged,
        isSubscription = isSubscription,
    )

    private fun BillingResult.toPurchaseResult(): PurchaseResult = when (responseCode) {
        BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseResult.Cancelled
        else -> toPurchaseError()
    }

    private fun BillingResult.toPurchaseError(): PurchaseResult.Error = PurchaseResult.Error(
        code = responseCode,
        message = debugMessage,
    )

    private data class CachedProduct(
        val details: ProductDetails,
        val type: String,
    )

    private data class SubscriptionUpdateTarget(
        val oldPurchaseToken: String,
        val oldProductId: String,
    )

    private companion object {
        const val MAX_CONNECTION_ATTEMPTS = 3

        val readyResult: BillingResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build()

        val disconnectedResult: BillingResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
            .setDebugMessage("Billing service is disconnected.")
            .build()
    }
}
