package by.tigre.media.platform.playback.eq.impl

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import by.tigre.media.platform.playback.eq.AudioRouteId
import by.tigre.media.platform.playback.eq.EqContentKey
import by.tigre.media.platform.playback.eq.EqProfile
import by.tigre.media.platform.playback.eq.EqProfileSource
import by.tigre.media.platform.playback.eq.db.DatabaseEqProfiles
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EqProfileRepositoryImplTest {

    private fun newRepo(): EqProfileRepositoryImpl {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val schema = DatabaseEqProfiles.Schema.synchronous()
        schema.create(driver)
        return EqProfileRepositoryImpl(DatabaseEqProfiles(driver))
    }

    @Test
    fun saveRespectsMaxTotalAndUpdatesCache() = runBlocking {
        val repo = newRepo()
        val a = EqProfile(
            id = 0,
            route = AudioRouteId.BuiltinSpeaker,
            content = EqContentKey.None,
            presetIndex = 1,
            gainsDb = listOf(0f, 1f),
            title = null,
            updatedAtMs = 1,
            source = EqProfileSource.Manual,
        )
        val b = a.copy(
            route = AudioRouteId.WiredHeadset,
            updatedAtMs = 2,
        )
        assertTrue(repo.save(a, maxAuto = 5, maxTotal = 1))
        assertEquals(1, repo.profiles.value.size)
        assertFalse(repo.save(b, maxAuto = 5, maxTotal = 1))
        assertEquals(1, repo.profiles.value.size)
        assertTrue(repo.save(a.copy(gainsDb = listOf(2f), updatedAtMs = 3), maxAuto = 5, maxTotal = 1))
        assertEquals(listOf(2f), repo.profiles.value.single().gainsDb)
    }

    @Test
    fun autoReplaceOldestWhenAtCap() = runBlocking {
        val repo = newRepo()
        repeat(2) { i ->
            assertTrue(
                repo.save(
                    EqProfile(
                        id = 0,
                        route = AudioRouteId.BuiltinSpeaker,
                        content = EqContentKey.Book(i.toLong()),
                        presetIndex = null,
                        gainsDb = listOf(i.toFloat()),
                        title = null,
                        updatedAtMs = i.toLong(),
                        source = EqProfileSource.Auto,
                    ),
                    maxAuto = 2,
                    maxTotal = 128,
                ),
            )
        }
        assertEquals(2, repo.profiles.value.size)
        assertTrue(
            repo.save(
                EqProfile(
                    id = 0,
                    route = AudioRouteId.BuiltinSpeaker,
                    content = EqContentKey.Book(99),
                    presetIndex = null,
                    gainsDb = listOf(9f),
                    title = null,
                    updatedAtMs = 10,
                    source = EqProfileSource.Auto,
                ),
                maxAuto = 2,
                maxTotal = 128,
            ),
        )
        assertEquals(2, repo.profiles.value.size)
        assertTrue(repo.profiles.value.none { it.content == EqContentKey.Book(0) })
        assertTrue(repo.profiles.value.any { it.content == EqContentKey.Book(99) })
    }

    @Test
    fun deleteRemovesFromCache() = runBlocking {
        val repo = newRepo()
        repo.save(
            EqProfile(
                id = 0,
                route = AudioRouteId.DesktopDefault,
                content = EqContentKey.Book(9),
                presetIndex = null,
                gainsDb = listOf(0f),
                title = "t",
                updatedAtMs = 1,
                source = EqProfileSource.Manual,
            ),
            maxAuto = 5,
            maxTotal = 8,
        )
        val id = repo.profiles.value.single().id
        repo.delete(id)
        assertTrue(repo.profiles.value.isEmpty())
    }
}
