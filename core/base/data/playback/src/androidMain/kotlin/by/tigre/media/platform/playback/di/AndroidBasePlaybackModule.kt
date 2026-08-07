package by.tigre.media.platform.playback.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import by.tigre.media.platform.playback.AndroidPlaybackPlayer
import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.PlaybackPlayer
import by.tigre.media.platform.playback.eq.AndroidAudioRouteMonitor
import by.tigre.media.platform.playback.eq.AudioRouteMonitor
import by.tigre.media.platform.playback.eq.EqContentKeyProvider
import by.tigre.media.platform.playback.eq.EqProfileController
import by.tigre.media.platform.playback.eq.EqProfileRepository
import by.tigre.media.platform.playback.eq.NullEqContentKeyProvider
import by.tigre.media.platform.playback.eq.db.DatabaseEqProfiles
import by.tigre.media.platform.playback.eq.impl.EqProfileRepositoryImpl
import by.tigre.media.platform.playback.impl.AndroidAppPlaybackVolume
import by.tigre.media.platform.playback.impl.AndroidPlaybackEqualizer
import by.tigre.media.platform.playback.impl.PlaybackPlayerImpl
import by.tigre.media.platform.playback.prefs.EqualizerPreferences
import by.tigre.media.platform.preferences.Preferences
import by.tigre.media.platform.tools.coroutines.CoroutineModule

class AndroidBasePlaybackModule(
    context: Context,
    coroutineModule: CoroutineModule,
    preferences: Preferences,
    contentKeyProvider: EqContentKeyProvider = NullEqContentKeyProvider(),
) : BasePlaybackModule {

    private val appContext = context.applicationContext
    private val equalizerPreferences = EqualizerPreferences(preferences)

    private val impl: PlaybackPlayerImpl by lazy {
        PlaybackPlayerImpl(
            context = appContext,
            scope = coroutineModule.scope,
        )
    }

    private val equalizer: AndroidPlaybackEqualizer by lazy {
        AndroidPlaybackEqualizer(impl, equalizerPreferences)
    }

    private val appPlaybackVolumeImpl: AndroidAppPlaybackVolume by lazy {
        AndroidAppPlaybackVolume(playerProvider = { impl.player as ExoPlayer })
    }

    private val eqDatabase: DatabaseEqProfiles by lazy {
        val schema = DatabaseEqProfiles.Schema.synchronous()
        val driver = AndroidSqliteDriver(
            schema = schema,
            context = appContext,
            name = "eq_profiles.db",
            callback = object : AndroidSqliteDriver.Callback(schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.execSQL("PRAGMA foreign_keys=ON;")
                }
            },
        )
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
        DatabaseEqProfiles(driver)
    }

    override val playbackPlayer: PlaybackPlayer get() = impl

    override val playbackEqualizer: PlaybackEqualizer get() = equalizer

    override val appPlaybackVolume: AppPlaybackVolume get() = appPlaybackVolumeImpl

    override val eqContentKeyProvider: EqContentKeyProvider = contentKeyProvider

    override val audioRouteMonitor: AudioRouteMonitor by lazy {
        AndroidAudioRouteMonitor(appContext)
    }

    override val eqProfileRepository: EqProfileRepository by lazy {
        EqProfileRepositoryImpl(eqDatabase)
    }

    override val eqProfileController: EqProfileController by lazy {
        EqProfileController(
            scope = coroutineModule.scope,
            repository = eqProfileRepository,
            routeMonitor = audioRouteMonitor,
            contentKeyProvider = eqContentKeyProvider,
            playbackEqualizer = equalizer,
            loadSuggestEnabled = { equalizerPreferences.loadSuggestSetup(true) },
            saveSuggestEnabled = { equalizerPreferences.saveSuggestSetup(it) },
        )
    }

    val androidPlaybackPlayer: AndroidPlaybackPlayer get() = impl
}
