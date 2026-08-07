package by.tigre.media.platform.entitlements

import by.tigre.media.platform.billing.OfferUi

fun visibleOffersForProduct(
    productId: String,
    offers: List<OfferUi>,
    tier: Tier,
    ownedBasePlanIds: Map<String, String>,
    plusSku: String,
    proSku: String,
): List<OfferUi> {
    if (productId == plusSku && tier.includes(Tier.Pro)) {
        return emptyList()
    }
    val ownsProduct = when (productId) {
        plusSku -> tier.includes(Tier.Plus)
        proSku -> tier.includes(Tier.Pro)
        else -> productId in ownedBasePlanIds
    }
    if (!ownsProduct) return offers
    return offers.filter { offer ->
        shouldShowOfferForOwnedSubscription(
            offerBasePlanId = offer.basePlanId,
            ownedBasePlanId = ownedBasePlanIds[productId],
        )
    }
}
