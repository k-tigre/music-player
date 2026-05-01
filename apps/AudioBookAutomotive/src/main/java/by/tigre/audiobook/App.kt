package by.tigre.audiobook

import android.app.Application
import by.tigre.audiobook.core.di.ApplicationGraph
import by.tigre.music.player.logger.CrashlyticsLogger
import by.tigre.music.player.logger.DbLogger
import by.tigre.music.player.logger.Log
import by.tigre.music.player.logger.LogDatabaseDriverFactory
import by.tigre.music.player.logger.LogcatLogger
import by.tigre.audiobook.BuildConfig

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
