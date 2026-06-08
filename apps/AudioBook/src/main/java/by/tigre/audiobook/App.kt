package by.tigre.audiobook

import android.app.Application
import by.tigre.audiobook.core.di.ApplicationGraph
import by.tigre.logger.CrashlyticsLogger
import by.tigre.logger.DbLogger
import by.tigre.logger.Log
import by.tigre.logger.LogDatabaseDriverFactory
import by.tigre.logger.LogcatLogger
import by.tigre.audiobook.analytics.FirebaseTracker
import by.tigre.music.player.tools.analytics.book.BookAnalyticsModuleImpl
import by.tigre.music.player.tools.analytics.LogTracker
import by.tigre.music.player.tools.analytics.MixpanelTracker
import by.tigre.music.player.tools.analytics.common.Tracker
import by.tigre.music.player.tools.coroutines.CoroutineModule
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
                backends.add(MixpanelTracker(this, BuildConfig.MIXPANEL_TOKEN, coroutineModule.scope))
            }
            Tracker.Aggregator(*backends.toTypedArray())
        } else {
            Tracker.Aggregator(LogTracker())
        }
        val analyticsModule = BookAnalyticsModuleImpl.create(tracker, coroutineModule)

        graph = ApplicationGraph.create(this, analyticsModule)
    }
}
