package by.tigre.media.platform.billing

/**
 * Minimal Play Billing presence so Console can unlock subscriptions / IAP setup.
 * Not an entitlements or purchase API — replace with real billing later.
 */
fun interface BillingWarmup {
    fun warmUp()
}
