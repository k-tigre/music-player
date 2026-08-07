package by.tigre.music.player.core.data.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaStoreVolumesTest {

    @Test
    fun resolveForRead_returnsNull_whenNoVolumes() {
        assertNull(MediaStoreVolumes.resolveForRead(emptySet()))
    }

    @Test
    fun resolveForRead_prefersSyntheticExternal_whenPrimaryPresent() {
        assertEquals(
            MediaStoreVolumes.EXTERNAL,
            MediaStoreVolumes.resolveForRead(
                setOf(MediaStoreVolumes.EXTERNAL_PRIMARY, "0000-0000")
            )
        )
    }

    @Test
    fun resolveForRead_usesOnlyAvailableVolume_whenPrimaryMissing() {
        assertEquals(
            "0000-0000",
            MediaStoreVolumes.resolveForRead(setOf("0000-0000"))
        )
    }

    @Test
    fun resolveForWrite_requiresPrimary() {
        assertEquals(
            MediaStoreVolumes.EXTERNAL_PRIMARY,
            MediaStoreVolumes.resolveForWrite(setOf(MediaStoreVolumes.EXTERNAL_PRIMARY))
        )
        assertNull(MediaStoreVolumes.resolveForWrite(setOf("0000-0000")))
        assertNull(MediaStoreVolumes.resolveForWrite(emptySet()))
    }
}
