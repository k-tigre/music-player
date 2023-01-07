package by.tigre.music.player.android

import android.app.Application
import by.tigre.music.player.android.core.di.ApplicationGraph

class App : Application() {
    lateinit var graph: ApplicationGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = ApplicationGraph.create()
    }
}
