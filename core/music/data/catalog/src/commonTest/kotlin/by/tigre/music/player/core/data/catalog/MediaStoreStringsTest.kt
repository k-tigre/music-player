package by.tigre.music.player.core.data.catalog

import kotlin.test.Test
import kotlin.test.assertEquals

class MediaStoreStringsTest {

    @Test
    fun nullArtistBecomesUnknownArtist() {
        assertEquals(
            MediaStoreStrings.UNKNOWN_ARTIST,
            MediaStoreStrings.orDefault(null, MediaStoreStrings.UNKNOWN_ARTIST),
        )
    }

    @Test
    fun blankArtistBecomesUnknownArtist() {
        assertEquals(
            MediaStoreStrings.UNKNOWN_ARTIST,
            MediaStoreStrings.orDefault("   ", MediaStoreStrings.UNKNOWN_ARTIST),
        )
    }

    @Test
    fun presentArtistIsPreserved() {
        assertEquals(
            "Pink Floyd",
            MediaStoreStrings.orDefault("Pink Floyd", MediaStoreStrings.UNKNOWN_ARTIST),
        )
    }

    @Test
    fun mediaStoreUnknownLiteralIsPreserved() {
        assertEquals(
            "<unknown>",
            MediaStoreStrings.orDefault("<unknown>", MediaStoreStrings.UNKNOWN_ARTIST),
        )
    }
}
