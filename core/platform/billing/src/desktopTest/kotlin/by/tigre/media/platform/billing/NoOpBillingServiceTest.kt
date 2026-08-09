package by.tigre.media.platform.billing

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NoOpBillingServiceTest {

    @Test
    fun purchasesAreCancelledAndNoProductsOrPurchasesAreExposed() = runBlocking {
        val service = NoOpBillingService

        service.start()
        service.queryProducts(listOf("tip"))

        assertNull(service.productDetails("tip").value)
        assertEquals(BillingStoreAvailability.Unavailable, service.storeAvailability.value)
        assertEquals(
            PurchaseResult.Cancelled,
            service.purchaseConsumable(BillingPurchaseHost(), "tip"),
        )
        assertEquals(emptyList(), service.queryActivePurchases())
    }
}
