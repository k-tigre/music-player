package by.tigre.music.player.core.data.storage.playback_queue.di

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import by.tigre.music.player.core.data.storage.music.DatabaseMusic
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.data.storage.playback_queue.impl.PlaybackQueueStorageImpl
import by.tigre.music.player.core.data.storage.playback_queue.impl.QueueStateAdapter
import by.tigre.music.player.core.data.storage.preferences.di.PreferencesModule
import by.tigre.music.player.tools.coroutines.CoroutineModule
import music.Queue
import java.io.File

class DesktopPlaybackQueueModule(
    dbDir: File?,
    coroutineModule: CoroutineModule,
    preferencesModule: PreferencesModule
) : PlaybackQueueModule {
    private val database: DatabaseMusic by lazy {
        val dbPath = if (dbDir != null) {
            dbDir.mkdirs()
            File(dbDir, "music.db").absolutePath
        } else {
            "music.db"
        }
        val driver = JdbcSqliteDriver("jdbc:sqlite:$dbPath")
        val schema = DatabaseMusic.Schema.synchronous()
        val currentVersion: Int = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor ->
                QueryResult.Value(
                    if (cursor.next().value) cursor.getLong(0)?.toInt() ?: 0 else 0
                )
            },
            parameters = 0
        ).value
        val tablesExist: Boolean = driver.executeQuery(
            identifier = null,
            sql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='Queue'",
            mapper = { cursor ->
                QueryResult.Value(
                    cursor.next().value && (cursor.getLong(0) ?: 0) > 0
                )
            },
            parameters = 0
        ).value
        if (currentVersion == 0 && !tablesExist) {
            schema.create(driver)
            driver.execute(null, "PRAGMA user_version=${schema.version}", 0)
        } else if (currentVersion < schema.version) {
            schema.migrate(driver, oldVersion = currentVersion.toLong(), newVersion = schema.version)
            driver.execute(null, "PRAGMA user_version=${schema.version}", 0)
        }
        DatabaseMusic(
            driver = driver,
            QueueAdapter = Queue.Adapter(QueueStateAdapter)
        )
    }

    override val playbackQueueStorage: PlaybackQueueStorage by lazy {
        PlaybackQueueStorageImpl(
            database = database,
            scope = coroutineModule.scope,
            preferences = preferencesModule.preferences
        )
    }
}
