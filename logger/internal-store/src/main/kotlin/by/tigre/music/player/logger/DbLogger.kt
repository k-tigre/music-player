package by.tigre.music.player.logger

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import by.tigre.music.player.logger.db.DatabaseLog
import bytigremusicplayerloggerdb.Logs
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

interface LogsProvider {
    suspend fun getLogs(offset: Long): List<Logs>
    suspend fun getLogsFlow(offset: Long): Flow<List<Logs>>
    suspend fun getLogs(offset: Long, tagFilter: String): List<Logs>
    suspend fun getLogsFlow(offset: Long, tagFilter: String): Flow<List<Logs>>
}

class DbLogger(
    context: Context
) : Log.Logger, LogsProvider {

    companion object {
        internal lateinit var dbLogger: DbLogger

        fun getLogsProvider() = dbLogger
    }

    init {
        dbLogger = this
    }

    private val db: DatabaseLog by lazy {
        DatabaseLog(
            AndroidSqliteDriver(
                schema = DatabaseLog.Schema,
                context = context,
                name = "logs.db",
                callback = object : AndroidSqliteDriver.Callback(DatabaseLog.Schema) {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.execSQL("PRAGMA foreign_keys=ON;")
                    }
                })
        )
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val entries = MutableSharedFlow<LogFileEntry>(
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        scope.launch {
            entries
                .onEach { entity ->
                    val fieldsMap = entity.fields.toMap()
                    val tag = fieldsMap[Log.Field.TAG]
                    val message = fieldsMap[Log.Field.MESSAGE] ?: fieldsMap.toString()
                    val otherFields = entity.fields.filter { (key, _) ->
                        key != Log.Field.MESSAGE
                                && key != Log.Field.STACKTRACE
                                && key != Log.Field.TAG
                                && key != Log.Field.THREAD
                                && key != Log.Field.TIMESTAMP
                    }.joinToString(
                        separator = "\n",
                        transform = { (key, value) -> "$key: $value" }
                    )
                    val stacktrace = fieldsMap[Log.Field.STACKTRACE]
                    val thread = fieldsMap[Log.Field.THREAD]
                    val timestamp = fieldsMap[Log.Field.TIMESTAMP]

                    db.logEntityQueries.insert(
                        message = message,
                        tag = tag,
                        otherFields = otherFields,
                        stacktrace = stacktrace,
                        thread = thread,
                        timestemp = try {
                            timestamp?.toLong() ?: System.currentTimeMillis()
                        } catch (_: Throwable) {
                            null
                        } ?: System.currentTimeMillis(),
                        level = entity.levelText
                    )
                }
                .collect()
        }

        scope.launch {
            delay(1.minutes)
            db.logEntityQueries.deleteOldest(System.currentTimeMillis() - 7.days.inWholeMilliseconds)
        }
    }

    override suspend fun getLogs(offset: Long): List<Logs> =
        db.logEntityQueries.get(limit = 1000L).executeAsList()

    override suspend fun getLogs(offset: Long, tagFilter: String): List<Logs> =
        db.logEntityQueries.getByTag(limit = 1000L, tag = tagFilter).executeAsList()

    override fun log(level: Log.Level, vararg fields: Pair<String, String>) {
        entries.tryEmit(LogFileEntry(level, fields))
    }

    override suspend fun getLogsFlow(offset: Long): Flow<List<Logs>> {
        return db.logEntityQueries.get(limit = 1000L).asFlow().mapToList()
    }

    override suspend fun getLogsFlow(offset: Long, tagFilter: String): Flow<List<Logs>> {
        return db.logEntityQueries.getByTag(limit = 1000L, tag = tagFilter).asFlow().mapToList()
    }
}
