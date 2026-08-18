package by.tigre.media.platform.entitlements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeatureModesTest {

    @Test
    fun missingModeDefaultsToOn() {
        assertEquals(FeatureMode.On, modeOrDefault(emptyMap(), Feature.Equalizer))
    }

    @Test
    fun parseModesIgnoresUnknownKeys() {
        val modes = parseFeatureModes(
            """{"equalizer":"paid","future_feature":"off","home_widget":"off"}""",
        )
        assertEquals(FeatureMode.Paid, modes[Feature.Equalizer])
        assertEquals(FeatureMode.Off, modes[Feature.HomeWidget])
        assertEquals(2, modes.size)
    }

    @Test
    fun invalidJsonYieldsEmptyModes() {
        assertTrue(parseFeatureModes("{not-json").isEmpty())
        assertTrue(parseFeatureModes("").isEmpty())
    }

    @Test
    fun unknownModeValueDefaultsToOn() {
        assertEquals(FeatureMode.On, FeatureMode.parse("maybe"))
        assertEquals(FeatureMode.On, FeatureMode.parse(null))
    }

    @Test
    fun onAllowsWithoutSubscription() {
        assertEquals(
            FeatureAccess.Allowed,
            resolveFeatureAccess(Feature.Equalizer, Tier.Free, FeatureMode.On),
        )
    }

    @Test
    fun offIsUnavailable() {
        assertEquals(
            FeatureAccess.Unavailable,
            resolveFeatureAccess(Feature.Equalizer, Tier.Pro, FeatureMode.Off),
        )
    }

    @Test
    fun paidRequiresTier() {
        assertEquals(
            FeatureAccess.RequiresPurchase,
            resolveFeatureAccess(Feature.HomeWidget, Tier.Plus, FeatureMode.Paid),
        )
        assertEquals(
            FeatureAccess.Allowed,
            resolveFeatureAccess(Feature.HomeWidget, Tier.Pro, FeatureMode.Paid),
        )
    }
}
