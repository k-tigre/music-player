package by.tigre.media.platform.playback.eq

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EqProfileResolverTest {

    private val bt = AudioRouteId(AudioRouteId.Kind.Bluetooth, "wh1000")
    private val speaker = AudioRouteId.BuiltinSpeaker

    private fun profile(
        id: Long,
        route: AudioRouteId,
        content: EqContentKey,
    ) = EqProfile(
        id = id,
        route = route,
        content = content,
        presetIndex = 0,
        gainsDb = listOf(1f, 0f),
        title = null,
        updatedAtMs = id,
    )

    @Test
    fun prefersBookOverFolderOverDevice() {
        val profiles = listOf(
            profile(1, bt, EqContentKey.None),
            profile(2, bt, EqContentKey.Folder("content://lib", "Author/Series")),
            profile(3, bt, EqContentKey.Book(42)),
        )
        val result = EqProfileResolver.resolveForBook(
            profiles = profiles,
            route = bt,
            bookId = 42,
            folderUri = "content://lib",
            subPath = "Author/Series",
        )
        assertEquals(EqMatchLevel.Book, result.matchLevel)
        assertEquals(3L, result.profile?.id)
    }

    @Test
    fun fallsBackToFolderThenDevice() {
        val profiles = listOf(
            profile(1, bt, EqContentKey.None),
            profile(2, bt, EqContentKey.Folder("content://lib", "Author/Series")),
        )
        val folderHit = EqProfileResolver.resolveForBook(
            profiles, bt, bookId = 99, folderUri = "content://lib", subPath = "Author/Series",
        )
        assertEquals(EqMatchLevel.Folder, folderHit.matchLevel)
        assertEquals(2L, folderHit.profile?.id)

        val deviceHit = EqProfileResolver.resolveForBook(
            profiles, bt, bookId = 99, folderUri = "content://lib", subPath = "Other",
        )
        assertEquals(EqMatchLevel.Device, deviceHit.matchLevel)
        assertEquals(1L, deviceHit.profile?.id)
    }

    @Test
    fun noMatchOnDifferentRoute() {
        val profiles = listOf(profile(1, bt, EqContentKey.None))
        val result = EqProfileResolver.resolve(profiles, speaker, EqContentKey.None)
        assertEquals(EqMatchLevel.None, result.matchLevel)
        assertNull(result.profile)
    }

    @Test
    fun contentKeyRoundTrip() {
        val folder = EqContentKey.Folder("content://lib", "a/b")
        val parsed = EqContentKey.fromStorage(folder.kind, folder.storageKey)
        assertEquals(folder, parsed)
        assertEquals(EqContentKey.Book(5), EqContentKey.fromStorage(EqContentKey.Kind.Book, "5"))
    }
}
