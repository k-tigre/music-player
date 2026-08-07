package by.tigre.media.platform.playback.di

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.PlaybackPlayer
import by.tigre.media.platform.playback.eq.AudioRouteMonitor
import by.tigre.media.platform.playback.eq.DesktopAudioRouteMonitor
import by.tigre.media.platform.playback.eq.EqContentKeyProvider
import by.tigre.media.platform.playback.eq.EqProfileController
import by.tigre.media.platform.playback.eq.EqProfileRepository
import by.tigre.media.platform.playback.eq.NullEqContentKeyProvider
import by.tigre.media.platform.playback.eq.db.DatabaseEqProfiles
import by.tigre.media.platform.playback.eq.impl.EqProfileRepositoryImpl
import by.tigre.media.platform.playback.impl.DesktopPlaybackEqualizer
import by.tigre.media.platform.playback.impl.FfmpegDesktopPlaybackPlayer
import by.tigre.media.platform.playback.impl.JdkClipDesktopPlaybackPlayer
import by.tigre.media.platform.playback.prefs.EqualizerPreferences
import by.tigre.media.platform.playback.prefs.PlaybackVolumePreferences
import by.tigre.media.platform.preferences.Preferences
import by.tigre.media.platform.tools.coroutines.CoroutineModule
import java.io.File

class DesktopBasePlaybackModule(
    preferences: Preferences,
    coroutineModule: CoroutineModule,
    dbDir: File? = null,
    contentKeyProvider: EqContentKeyProvider = NullEqContentKeyProvider(),
) : BasePlaybackModule {

    private val equalizerPreferences = EqualizerPreferences(preferences)
    private val volumePreferences = PlaybackVolumePreferences(preferences)

    private val ffmpegPlayer = FfmpegDesktopPlaybackPlayer.tryCreate(equalizerPreferences, volumePreferences)

    private val jdkPlayer = JdkClipDesktopPlaybackPlayer(volumePreferences).takeIf { ffmpegPlayer == null }

    override val playbackPlayer: PlaybackPlayer = ffmpegPlayer ?: jdkPlayer!!

    override val playbackEqualizer: PlaybackEqualizer =
        ffmpegPlayer ?: DesktopPlaybackEqualizer(jdkPlayer!!, equalizerPreferences)

    override val appPlaybackVolume: AppPlaybackVolume =
        (ffmpegPlayer ?: jdkPlayer!!) as AppPlaybackVolume

    override val eqContentKeyProvider: EqContentKeyProvider = contentKeyProvider

    override val audioRouteMonitor: AudioRouteMonitor = DesktopAudioRouteMonitor()

    private val eqDatabase: DatabaseEqProfiles by lazy {
        val dbPath = if (dbDir != null) {
            dbDir.mkdirs()
            File(dbDir, "eq_profiles.db").absolutePath
        } else {
            "eq_profiles.db"
        }
        val driver = JdbcSqliteDriver("jdbc:sqlite:$dbPath")
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
            driver.execute(null, "PRAGMA user_version=${schema.version}", 0)
        } else if (currentVersion < schema.version) {
            schema.migrate(driver, oldVersion = currentVersion.toLong(), newVersion = schema.version)
            driver.execute(null, "PRAGMA user_version=${schema.version}", 0)
        }
        DatabaseEqProfiles(driver)
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
            playbackEqualizer = playbackEqualizer,
            suggestEnabled = { equalizerPreferences.loadSuggestSetup(true) },
        )
    }
}
