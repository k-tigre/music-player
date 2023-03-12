package by.tigre.music.player.core.data.storage.playback_queue.di

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import by.tigre.music.player.core.data.storage.music.DatabaseMusic
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.data.storage.playback_queue.impl.PlaybackQueueStorageImpl
import by.tigre.music.player.core.data.storage.playback_queue.impl.QueueStateAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver
import kotlinx.coroutines.CoroutineScope
import music.Queue

interface PlaybackQueueModule {
    val playbackQueueStorage: PlaybackQueueStorage

    class Impl(
        context: Context,
        scope: CoroutineScope
    ) : PlaybackQueueModule {
        private val database: DatabaseMusic by lazy {
            DatabaseMusic(
                driver = AndroidSqliteDriver(
                    schema = DatabaseMusic.Schema,
                    context = context,
                    name = "music.db",
                    callback = object : AndroidSqliteDriver.Callback(DatabaseMusic.Schema) {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            db.execSQL("PRAGMA foreign_keys=ON;");
                        }
                    }),
                QueueAdapter = Queue.Adapter(QueueStateAdapter)
            )
        }

        override val playbackQueueStorage: PlaybackQueueStorage by lazy { PlaybackQueueStorageImpl(database, scope) }

    }
}
