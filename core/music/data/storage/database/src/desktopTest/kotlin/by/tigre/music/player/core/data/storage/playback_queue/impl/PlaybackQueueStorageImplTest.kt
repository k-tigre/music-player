package by.tigre.music.player.core.data.storage.playback_queue.impl

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import by.tigre.media.platform.preferences.Preferences
import by.tigre.music.player.core.data.storage.music.DatabaseMusic
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.data.storage.playlist.impl.PlaylistKindAdapter
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.entiry.playback.QueueSession
import by.tigre.music.player.core.entiry.playlist.Playlist as PlaylistEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import music.Playlist
import music.Queue
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackQueueStorageImplTest {

    @Test
    fun queueSessionRoundTripsThroughPreferences() = runBlocking {
        val preferences = FakePreferences()
        val storage = createStorage(preferences)

        storage.saveQueueSession(
            QueueSession.FromPlaylist(
                playlistId = PlaylistEntity.Id(7),
                name = "Evening",
                isDirty = true,
            )
        )

        assertEquals(
            QueueSession.FromPlaylist(
                playlistId = PlaylistEntity.Id(7),
                name = "Evening",
                isDirty = true,
            ),
            storage.loadQueueSession(),
        )

        storage.saveQueueSession(QueueSession.Plain)

        assertEquals(QueueSession.Plain, storage.loadQueueSession())
    }

    @Test
    fun disablingShuffleNormalizesStatusesToNaturalOrder() = runBlocking {
        val context = createStorageContext(FakePreferences())
        val storage = context.storage

        storage.playSongs((1L..11L).map(::songId))
        storage.setShuffleEnabled(true)
        context.driver.execute(
            identifier = null,
            sql = "UPDATE Queue SET sort_order = song_id",
            parameters = 0,
        )
        context.driver.execute(
            identifier = null,
            sql = """
                UPDATE Queue
                SET status = CASE song_id
                    WHEN 1 THEN 2
                    WHEN 2 THEN 2
                    WHEN 3 THEN 2
                    WHEN 4 THEN 2
                    WHEN 5 THEN 1
                    WHEN 6 THEN 0
                    WHEN 7 THEN 0
                    WHEN 8 THEN 0
                    WHEN 9 THEN 0
                    WHEN 10 THEN 2
                    WHEN 11 THEN 0
                    ELSE status
                END
            """.trimIndent(),
            parameters = 0,
        )

        storage.setShuffleEnabled(false)

        assertEquals(listOf(6L, 7L, 8L, 9L, 10L, 11L), buildList {
            repeat(6) {
                storage.playNext()
                add(context.database.currentPlayingSongId())
            }
        })
    }

    private data class StorageTestContext(
        val storage: PlaybackQueueStorageImpl,
        val database: DatabaseMusic,
        val driver: JdbcSqliteDriver,
    )

    private fun createStorage(preferences: FakePreferences): PlaybackQueueStorageImpl =
        createStorageContext(preferences).storage

    private fun createStorageContext(preferences: FakePreferences): StorageTestContext {
        val driver = JdbcSqliteDriver("jdbc:sqlite::memory:")
        DatabaseMusic.Schema.synchronous().create(driver)
        val database = DatabaseMusic(
            driver = driver,
            QueueAdapter = Queue.Adapter(QueueStateAdapter),
            PlaylistAdapter = Playlist.Adapter(PlaylistKindAdapter),
        )
        return StorageTestContext(
            storage = PlaybackQueueStorageImpl(
                database = database,
                preferences = preferences,
                scope = CoroutineScope(Job() + Dispatchers.Unconfined),
            ),
            database = database,
            driver = driver,
        )
    }

    private fun DatabaseMusic.currentPlayingSongId(): Long =
        queueQueries.selectAllByOrder(limit = 10000) { _, status, songId ->
            PlaybackQueueStorage.QueueItem(id = 0, songsId = Song.Id(songId), state = status)
        }.executeAsList().first { it.state == PlaybackQueueStorage.QueueItem.State.Playing }.songsId.value

    private fun songId(value: Long) = Song.Id(value)

    private class FakePreferences : Preferences {
        private val booleans = mutableMapOf<String, Boolean>()
        private val strings = mutableMapOf<String, String?>()
        private val ints = mutableMapOf<String, Int?>()
        private val longs = mutableMapOf<String, Long?>()

        override fun saveBoolean(key: String, value: Boolean) {
            booleans[key] = value
        }

        override fun saveBooleans(vararg values: Pair<String, Boolean>) {
            values.forEach { (key, value) -> booleans[key] = value }
        }

        override fun loadBoolean(key: String, default: Boolean): Boolean = booleans[key] ?: default

        override fun saveString(key: String, value: String?) {
            strings[key] = value
        }

        override fun loadString(key: String, default: String?): String? = strings[key] ?: default

        override fun saveInt(key: String, value: Int?) {
            ints[key] = value
        }

        override fun loadInt(key: String, default: Int): Int = ints[key] ?: default

        override fun saveLong(key: String, value: Long?) {
            longs[key] = value
        }

        override fun loadLong(key: String, default: Long): Long = longs[key] ?: default
    }
}
