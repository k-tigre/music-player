package by.tigre.media.platform.billing

object NoOpBillingWarmup : BillingWarmup {
    override fun warmUp() = Unit
}
