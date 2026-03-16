package by.tigre.music.player.logger

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import by.tigre.music.player.logger.db.DatabaseLog

object LogDatabaseDriverFactory {
    fun create(context: Context): SqlDriver {
        return AndroidSqliteDriver(
            schema = DatabaseLog.Schema.synchronous(),
            context = context,
            name = "logs.db",
            callback = object : AndroidSqliteDriver.Callback(DatabaseLog.Schema.synchronous()) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.execSQL("PRAGMA foreign_keys=ON;")
                }
            }
        )
    }
}
