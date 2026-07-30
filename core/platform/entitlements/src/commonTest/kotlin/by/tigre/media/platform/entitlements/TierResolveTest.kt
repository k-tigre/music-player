package by.tigre.media.platform.entitlements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TierResolveTest {

    @Test
    fun proIncludesPlus() {
        assertTrue(Tier.Pro.includes(Tier.Plus))
        assertTrue(Tier.Pro.includes(Tier.Pro))
        assertFalse(Tier.Plus.includes(Tier.Pro))
    }

    @Test
    fun freePlaylistLimitIs3() {
        assertEquals(3, EntitlementLimits.defaultPlaylistLimit(Tier.Free))
    }

    @Test
    fun homeWidgetRequiresPro() {
        assertEquals(Tier.Pro, Feature.HomeWidget.minTier())
    }

    @Test
    fun equalizerRequiresPlus() {
        assertEquals(Tier.Plus, Feature.Equalizer.minTier())
    }
}
