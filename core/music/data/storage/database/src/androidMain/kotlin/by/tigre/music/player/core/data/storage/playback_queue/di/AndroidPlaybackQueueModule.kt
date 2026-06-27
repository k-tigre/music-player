package by.tigre.music.player.core.data.storage.playback_queue.di

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import by.tigre.music.player.core.data.storage.music.DatabaseMusic
import by.tigre.music.player.core.data.storage.favorites.FavoritesStorage
import by.tigre.music.player.core.data.storage.favorites.impl.FavoritesStorageImpl
import by.tigre.music.player.core.data.storage.playlist.PlaylistStorage
import by.tigre.music.player.core.data.storage.playlist.impl.PlaylistKindAdapter
import by.tigre.music.player.core.data.storage.playlist.impl.PlaylistStorageImpl
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.data.storage.playback_queue.impl.PlaybackQueueStorageImpl
import by.tigre.music.player.core.data.storage.playback_queue.impl.QueueStateAdapter
import by.tigre.media.platform.preferences.di.PreferencesModule
import by.tigre.media.platform.tools.coroutines.CoroutineModule
import music.Playlist
import music.Queue

class AndroidPlaybackQueueModule(
    context: Context,
    coroutineModule: CoroutineModule,
    preferencesModule: PreferencesModule
) : PlaybackQueueModule {
    private val database: DatabaseMusic by lazy {
        val schema = DatabaseMusic.Schema.synchronous()
        val driver = AndroidSqliteDriver(
            schema = schema,
            context = context,
            name = "music.db",
            callback = object : AndroidSqliteDriver.Callback(schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.execSQL("PRAGMA foreign_keys=ON;")
                }
            }
        )
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
            driver.execute(identifier = null, sql = "PRAGMA user_version=${schema.version}", parameters = 0)
        } else if (currentVersion < schema.version) {
            schema.migrate(driver, oldVersion = currentVersion.toLong(), newVersion = schema.version)
            driver.execute(identifier = null, sql = "PRAGMA user_version=${schema.version}", parameters = 0)
        }
        DatabaseMusic(
            driver = driver,
            QueueAdapter = Queue.Adapter(QueueStateAdapter),
            PlaylistAdapter = Playlist.Adapter(PlaylistKindAdapter),
        )
    }

    override val playbackQueueStorage: PlaybackQueueStorage by lazy {
        PlaybackQueueStorageImpl(
            database = database,
            scope = coroutineModule.scope,
            preferences = preferencesModule.preferences
        )
    }

    override val playlistStorage: PlaylistStorage by lazy {
        PlaylistStorageImpl(
            database = database,
            scope = coroutineModule.scope,
        )
    }

    override val favoritesStorage: FavoritesStorage by lazy {
        FavoritesStorageImpl(
            database = database,
            scope = coroutineModule.scope,
        )
    }
}
