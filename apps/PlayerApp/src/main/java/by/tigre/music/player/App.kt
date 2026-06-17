package by.tigre.music.player

import android.app.Application
import by.tigre.music.player.core.di.ApplicationGraph
import by.tigre.logger.CrashlyticsLogger
import by.tigre.logger.DbLogger
import by.tigre.logger.Log
import by.tigre.logger.LogDatabaseDriverFactory
import by.tigre.logger.LogcatLogger
import by.tigre.music.player.analytics.FirebaseTracker
import by.tigre.media.platform.tools.analytics.LogTracker
import by.tigre.media.platform.tools.analytics.MixpanelTracker
import by.tigre.media.platform.tools.analytics.music.MusicAnalyticsModuleImpl
import by.tigre.media.platform.tools.analytics.common.Tracker
import by.tigre.media.platform.tools.coroutines.CoroutineModule
import com.google.firebase.FirebaseApp

class App : Application() {
    lateinit var graph: ApplicationGraph
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Log.init(
                Log.Level.VERBOSE,
                LogcatLogger(),
                CrashlyticsLogger(),
                DbLogger(LogDatabaseDriverFactory.create(this), android.os.Process.myPid())
            )
        } else {
            Log.init(Log.Level.DEBUG, CrashlyticsLogger())
        }

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        val coroutineModule = CoroutineModule.Impl()
        val tracker = if (BuildConfig.REMOTE_ANALYTICS_ENABLED) {
            val backends = mutableListOf<Tracker>(
                LogTracker(),
                FirebaseTracker(this),
            )
            if (BuildConfig.MIXPANEL_TOKEN.isNotBlank()) {
                backends.add(
                    MixpanelTracker(
                        context = this,
                        mixpanelToken = BuildConfig.MIXPANEL_TOKEN,
                        serverUrl = BuildConfig.MIXPANEL_SERVER_URL,
                        scope = coroutineModule.scope,
                    )
                )
            }
            Tracker.Aggregator(*backends.toTypedArray())
        } else {
            Tracker.Aggregator(LogTracker())
        }
        val analyticsModule = MusicAnalyticsModuleImpl.create(tracker, coroutineModule)

        graph = ApplicationGraph.create(this, analyticsModule)
    }
}
