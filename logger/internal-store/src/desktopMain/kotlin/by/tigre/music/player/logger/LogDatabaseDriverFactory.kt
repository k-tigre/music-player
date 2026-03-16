package by.tigre.music.player.logger

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import by.tigre.music.player.logger.db.DatabaseLog
import java.io.File

object LogDatabaseDriverFactory {
    fun create(dbDir: File? = null): SqlDriver {
        val dbPath = if (dbDir != null) {
            dbDir.mkdirs()
            File(dbDir, "logs.db").absolutePath
        } else {
            "logs.db"
        }
        val driver = JdbcSqliteDriver("jdbc:sqlite:$dbPath")
        val schema = DatabaseLog.Schema.synchronous()
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
            sql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='Logs'",
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
        return driver
    }
}
