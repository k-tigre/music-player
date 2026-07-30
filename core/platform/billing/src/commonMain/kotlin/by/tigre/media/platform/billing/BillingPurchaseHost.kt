package by.tigre.media.platform.billing

/**
 * Platform-owned UI host used to launch a purchase flow.
 *
 * Android callers create an instance from their current [android.app.Activity]; desktop has a
 * no-op implementation so shared callers do not need an Android dependency.
 */
expect class BillingPurchaseHost
