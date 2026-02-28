package by.tigre.audiobook.core.data.storage.audiobook_catalog.di

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import by.tigre.audiobook.core.data.storage.audiobook.DatabaseAudiobook
import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage
import by.tigre.audiobook.core.data.storage.audiobook_catalog.impl.AudiobookCatalogStorageImpl
import by.tigre.music.player.tools.coroutines.CoroutineModule

interface AudiobookCatalogStorageModule {
    val audiobookCatalogStorage: AudiobookCatalogStorage

    class Impl(
        context: Context,
        coroutineModule: CoroutineModule
    ) : AudiobookCatalogStorageModule {
        private val database: DatabaseAudiobook by lazy {
            DatabaseAudiobook(
                driver = AndroidSqliteDriver(
                    schema = DatabaseAudiobook.Schema.synchronous(),
                    context = context,
                    name = "audiobook.db",
                    callback = object : AndroidSqliteDriver.Callback(DatabaseAudiobook.Schema.synchronous()) {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            db.execSQL("PRAGMA foreign_keys=ON;")
                        }
                    }
                )
            )
        }

        override val audiobookCatalogStorage: AudiobookCatalogStorage by lazy {
            AudiobookCatalogStorageImpl(
                database = database,
                scope = coroutineModule.scope
            )
        }
    }
}
