package by.tigre.music.player

import android.app.Application
import by.tigre.music.player.core.di.ApplicationGraph
import by.tigre.logger.CrashlyticsLogger
import by.tigre.logger.DbLogger
import by.tigre.logger.Log
import by.tigre.logger.LogDatabaseDriverFactory
import by.tigre.logger.LogcatLogger
import by.tigre.music.player.BuildConfig

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

        graph = ApplicationGraph.create(this)
    }
}
