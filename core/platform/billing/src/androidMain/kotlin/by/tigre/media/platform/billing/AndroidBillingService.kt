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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidBillingService(
    context: Context,
) : BillingService {
    private val appContext = context.applicationContext
    private val productFlows = ConcurrentHashMap<String, MutableStateFlow<ProductUi?>>()
    private val cachedProducts = ConcurrentHashMap<String, CachedProduct>()
    private val connectionMutex = Mutex()
    private val purchaseMutex = Mutex()

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
    ): PurchaseResult = launchPurchase(
        host = host,
        product = cachedProducts[productId],
        offerToken = offerToken,
        expectedType = BillingClient.ProductType.SUBS,
    )

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
        suspendCoroutine { continuation ->
            client.acknowledgePurchase(params) {
                continuation.resume(Unit)
            }
        }
    }

    override suspend fun consumeTip(purchase: PurchaseSnapshot) {
        if (purchase.isSubscription) return

        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        suspendCoroutine { continuation ->
            client.consumeAsync(params) { _, _ ->
                continuation.resume(Unit)
            }
        }
    }

    private suspend fun launchPurchase(
        host: BillingPurchaseHost,
        product: CachedProduct?,
        offerToken: String?,
        expectedType: String,
    ): PurchaseResult = purchaseMutex.withLock {
        val connectionResult = ensureStarted()
        if (connectionResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return@withLock connectionResult.toPurchaseError()
        }
        if (product == null || product.type != expectedType) {
            return@withLock PurchaseResult.Error(
                code = BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                message = "Product details are unavailable. Call queryProducts before purchasing.",
            )
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product.details)
            .apply { offerToken?.let(::setOfferToken) }
            .build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val deferred = CompletableDeferred<PurchaseResult>()
        completedPurchase = null
        pendingPurchase = deferred
        try {
            val launchResult = client.launchBillingFlow(host.activity, params)
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                deferred.complete(launchResult.toPurchaseResult())
            }
            deferred.await()
        } finally {
            pendingPurchase = null
        }
    }

    private suspend fun ensureStarted(): BillingResult {
        if (client.isReady) return readyResult

        var lastResult = disconnectedResult
        repeat(MAX_CONNECTION_ATTEMPTS) {
            val deferred = connectionMutex.withLock {
                if (client.isReady) return readyResult
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
                return result
            }
            connectionMutex.withLock {
                if (connection === deferred) {
                    connection = null
                }
            }
        }
        return lastResult
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
        val result = suspendCoroutine { continuation ->
            client.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
                continuation.resume(billingResult to productDetailsResult.productDetailsList)
            }
        }
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
        return suspendCoroutine { continuation ->
            client.queryPurchasesAsync(params) { billingResult, purchases ->
                continuation.resume(
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        purchases
                    } else {
                        emptyList()
                    },
                )
            }
        }
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
