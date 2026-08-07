package by.tigre.media.platform.entitlements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionPeriodTest {

    @Test
    fun parsesConsoleStyleBasePlanIds() {
        assertEquals(SubscriptionPeriod.Yearly, parseSubscriptionPeriod("year-music-plus"))
        assertEquals(SubscriptionPeriod.Monthly, parseSubscriptionPeriod("month-music-plus"))
        assertEquals(SubscriptionPeriod.Yearly, parseSubscriptionPeriod("yearly"))
        assertEquals(SubscriptionPeriod.Monthly, parseSubscriptionPeriod("monthly"))
    }

    @Test
    fun monthlyOwnedHidesMonthlyKeepsYearly() {
        assertFalse(
            shouldShowOfferForOwnedSubscription(
                offerBasePlanId = "month-music-plus",
                ownedBasePlanId = "month-music-plus",
            ),
        )
        assertTrue(
            shouldShowOfferForOwnedSubscription(
                offerBasePlanId = "year-music-plus",
                ownedBasePlanId = "month-music-plus",
            ),
        )
    }

    @Test
    fun yearlyOwnedHidesAllOffers() {
        assertFalse(
            shouldShowOfferForOwnedSubscription(
                offerBasePlanId = "month-music-plus",
                ownedBasePlanId = "year-music-plus",
            ),
        )
        assertFalse(
            shouldShowOfferForOwnedSubscription(
                offerBasePlanId = "year-music-plus",
                ownedBasePlanId = "year-music-plus",
            ),
        )
    }
}
