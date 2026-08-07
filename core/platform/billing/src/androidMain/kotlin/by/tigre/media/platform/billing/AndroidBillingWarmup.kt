package by.tigre.media.platform.billing

import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener

class AndroidBillingWarmup(
    context: Context,
) : BillingWarmup {
    private val appContext = context.applicationContext

    override fun warmUp() {
        val purchasesUpdatedListener = PurchasesUpdatedListener { _, _ ->
            // Stub: no purchase flow yet.
        }
        val client = BillingClient.newBuilder(appContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()

        client.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    Log.i(
                        TAG,
                        "Billing warmup finished code=${billingResult.responseCode} " +
                            "debugMessage=${billingResult.debugMessage}",
                    )
                    client.endConnection()
                }

                override fun onBillingServiceDisconnected() {
                    Log.d(TAG, "Billing service disconnected during warmup")
                }
            }
        )
    }

    private companion object {
        const val TAG = "BillingWarmup"
    }
}
