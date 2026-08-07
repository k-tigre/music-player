package by.tigre.media.platform.billing

import android.app.Activity

actual class BillingPurchaseHost private constructor(
    internal val activity: Activity,
) {
    companion object {
        fun from(activity: Activity): BillingPurchaseHost = BillingPurchaseHost(activity)
    }
}
