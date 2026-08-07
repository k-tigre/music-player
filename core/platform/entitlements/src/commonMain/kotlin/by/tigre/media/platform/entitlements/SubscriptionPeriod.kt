package by.tigre.media.platform.entitlements

enum class SubscriptionPeriod {
    Monthly,
    Yearly,
    Unknown,
}

/**
 * Console base-plan IDs: `{monthly|yearly}-{productId}` e.g. `yearly-music-pro`.
 * Also accepts bare `monthly` / `yearly` and legacy `month-*` / `year-*`.
 */
fun parseSubscriptionPeriod(basePlanId: String?): SubscriptionPeriod {
    val id = basePlanId?.lowercase().orEmpty()
    if (id.isEmpty()) return SubscriptionPeriod.Unknown
    return when {
        id == "yearly" || id == "year" || id == "annual" ||
            id.startsWith("yearly-") || id.startsWith("year-") || id.startsWith("annual") ||
            id.contains("year") || id.contains("annual") ->
            SubscriptionPeriod.Yearly

        id == "monthly" || id == "month" ||
            id.startsWith("monthly-") || id.startsWith("month-") ||
            id.contains("month") ->
            SubscriptionPeriod.Monthly

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
