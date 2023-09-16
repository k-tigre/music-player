package by.tigre.music.player

import android.app.Application
import by.tigre.music.player.core.di.ApplicationGraph
import by.tigre.music.player.logger.CrashlyticsLogger
import by.tigre.music.player.logger.DbLogger
import by.tigre.music.player.logger.Log
import by.tigre.music.player.logger.LogcatLogger

class App : Application() {
    lateinit var graph: ApplicationGraph
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Log.init(Log.Level.VERBOSE, LogcatLogger(), CrashlyticsLogger(), DbLogger(this))
        } else {
            Log.init(Log.Level.DEBUG, CrashlyticsLogger())
        }

        graph = ApplicationGraph.create(this)
    }
}
