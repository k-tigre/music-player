package by.tigre.media.platform.entitlements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionPeriodTest {

    @Test
    fun parsesConsoleStyleBasePlanIds() {
        assertEquals(SubscriptionPeriod.Yearly, parseSubscriptionPeriod("yearly-music-pro"))
        assertEquals(SubscriptionPeriod.Monthly, parseSubscriptionPeriod("monthly-music-plus"))
        assertEquals(SubscriptionPeriod.Yearly, parseSubscriptionPeriod("yearly-audiobook-pro"))
        assertEquals(SubscriptionPeriod.Monthly, parseSubscriptionPeriod("monthly-audiobook-plus"))
        assertEquals(SubscriptionPeriod.Yearly, parseSubscriptionPeriod("yearly"))
        assertEquals(SubscriptionPeriod.Monthly, parseSubscriptionPeriod("monthly"))
    }

    @Test
    fun parsesLegacyShortPrefixes() {
        assertEquals(SubscriptionPeriod.Yearly, parseSubscriptionPeriod("year-music-plus"))
        assertEquals(SubscriptionPeriod.Monthly, parseSubscriptionPeriod("month-music-plus"))
    }

    @Test
    fun monthlyOwnedHidesMonthlyKeepsYearly() {
        assertFalse(
            shouldShowOfferForOwnedSubscription(
                offerBasePlanId = "monthly-music-plus",
                ownedBasePlanId = "monthly-music-plus",
            ),
        )
        assertTrue(
            shouldShowOfferForOwnedSubscription(
                offerBasePlanId = "yearly-music-plus",
                ownedBasePlanId = "monthly-music-plus",
            ),
        )
    }

    @Test
    fun yearlyOwnedHidesAllOffers() {
        assertFalse(
            shouldShowOfferForOwnedSubscription(
                offerBasePlanId = "monthly-music-pro",
                ownedBasePlanId = "yearly-music-pro",
            ),
        )
        assertFalse(
            shouldShowOfferForOwnedSubscription(
                offerBasePlanId = "yearly-music-pro",
                ownedBasePlanId = "yearly-music-pro",
            ),
        )
    }
}
