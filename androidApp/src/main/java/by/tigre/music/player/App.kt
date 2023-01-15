package by.tigre.music.player

import android.app.Application
import by.tigre.music.player.core.di.ApplicationGraph

class App : Application() {
    lateinit var graph: ApplicationGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = ApplicationGraph.create()
    }
}
