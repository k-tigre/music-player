package by.tigre.media.platform.entitlements

enum class SubscriptionPeriod {
    Monthly,
    Yearly,
    Unknown,
}

fun parseSubscriptionPeriod(basePlanId: String?): SubscriptionPeriod {
    val id = basePlanId?.lowercase().orEmpty()
    if (id.isEmpty()) return SubscriptionPeriod.Unknown
    val yearly = id == "yearly" || id == "year" || id == "annual" ||
        id.contains("year") || id.contains("annual")
    val monthly = id == "monthly" || id == "month" || id.contains("month")
    return when {
        yearly -> SubscriptionPeriod.Yearly
        monthly -> SubscriptionPeriod.Monthly
        else -> SubscriptionPeriod.Unknown
    }
}

/**
 * Whether an offer should stay visible for an already-owned subscription product.
 * Monthly owned → hide monthly (yearly upgrade stays). Yearly owned → hide all.
 * Unknown period → hide monthly only (keeps yearly upgrade after restore).
 */
fun shouldShowOfferForOwnedSubscription(
    offerBasePlanId: String?,
    ownedBasePlanId: String?,
): Boolean =
    when (parseSubscriptionPeriod(ownedBasePlanId)) {
        SubscriptionPeriod.Yearly -> false
        SubscriptionPeriod.Monthly,
        SubscriptionPeriod.Unknown,
        -> parseSubscriptionPeriod(offerBasePlanId) != SubscriptionPeriod.Monthly
    }
