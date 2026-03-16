package by.tigre.audiobook.core.data.storage.audiobook_catalog.di

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import by.tigre.audiobook.core.data.storage.audiobook.DatabaseAudiobook
import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage
import by.tigre.audiobook.core.data.storage.audiobook_catalog.impl.AudiobookCatalogStorageImpl
import by.tigre.audiobook.core.data.storage.audiobook_playback.AudiobookPlaybackStorage
import by.tigre.audiobook.core.data.storage.audiobook_playback.impl.AudiobookPlaybackStorageImpl
import by.tigre.music.player.tools.coroutines.CoroutineModule
import java.io.File

class DesktopAudiobookCatalogStorageModule(
    dbDir: File?,
    coroutineModule: CoroutineModule
) : AudiobookCatalogStorageModule {
    private val database: DatabaseAudiobook by lazy {
        val dbPath = if (dbDir != null) {
            dbDir.mkdirs()
            File(dbDir, "audiobook.db").absolutePath
        } else {
            "audiobook.db"
        }
        val driver = JdbcSqliteDriver("jdbc:sqlite:$dbPath")
        val schema = DatabaseAudiobook.Schema.synchronous()
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
            sql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='Book'",
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
        DatabaseAudiobook(driver = driver)
    }

    override val audiobookCatalogStorage: AudiobookCatalogStorage by lazy {
        AudiobookCatalogStorageImpl(
            database = database,
            scope = coroutineModule.scope
        )
    }

    override val audiobookPlaybackStorage: AudiobookPlaybackStorage by lazy {
        AudiobookPlaybackStorageImpl(database = database)
    }
}
