package by.tigre.music.player.core.data.storage.playback_queue.di

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import by.tigre.music.player.core.data.storage.music.DatabaseMusic
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.data.storage.playback_queue.impl.PlaybackQueueStorageImpl
import by.tigre.music.player.core.data.storage.playback_queue.impl.QueueStateAdapter
import by.tigre.music.player.tools.coroutines.CoroutineModule
import music.Queue

interface PlaybackQueueModule {
    val playbackQueueStorage: PlaybackQueueStorage

    class Impl(
        context: Context,
        coroutineModule: CoroutineModule
    ) : PlaybackQueueModule {
        private val database: DatabaseMusic by lazy {
            DatabaseMusic(
                driver = AndroidSqliteDriver(
                    schema = DatabaseMusic.Schema.synchronous(),
                    context = context,
                    name = "music.db",
                    callback = object : AndroidSqliteDriver.Callback(DatabaseMusic.Schema.synchronous()) {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            db.execSQL("PRAGMA foreign_keys=ON;");
                        }
                    }),
                QueueAdapter = Queue.Adapter(QueueStateAdapter)
            )
        }

        override val playbackQueueStorage: PlaybackQueueStorage by lazy {
            PlaybackQueueStorageImpl(
                database = database,
                scope = coroutineModule.scope
            )
        }

    }
}
