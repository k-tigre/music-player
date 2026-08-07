package by.tigre.media.platform.playback.eq.impl

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import by.tigre.media.platform.playback.eq.db.DatabaseEqProfiles

internal fun openEqProfilesDatabase(driver: SqlDriver): DatabaseEqProfiles {
    val schema = DatabaseEqProfiles.Schema.synchronous()
    val currentVersion: Int = driver.executeQuery(
        identifier = null,
        sql = "PRAGMA user_version",
        mapper = { cursor ->
            QueryResult.Value(
                if (cursor.next().value) cursor.getLong(0)?.toInt() ?: 0 else 0
            )
        },
        parameters = 0,
    ).value
    val tablesExist: Boolean = driver.executeQuery(
        identifier = null,
        sql = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='EqProfileRow'",
        mapper = { cursor ->
            QueryResult.Value(
                cursor.next().value && (cursor.getLong(0) ?: 0) > 0
            )
        },
        parameters = 0,
    ).value
    if (currentVersion == 0 && !tablesExist) {
        schema.create(driver)
        driver.execute(identifier = null, sql = "PRAGMA user_version=${schema.version}", parameters = 0)
    } else if (currentVersion < schema.version) {
        schema.migrate(driver, oldVersion = currentVersion.toLong(), newVersion = schema.version)
        driver.execute(identifier = null, sql = "PRAGMA user_version=${schema.version}", parameters = 0)
    }
    return DatabaseEqProfiles(driver)
}
