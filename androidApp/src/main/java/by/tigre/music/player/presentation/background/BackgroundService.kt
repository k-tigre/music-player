package by.tigre.music.player.presentation.background

import android.app.Service
import android.content.Intent
import android.os.IBinder
import by.tigre.music.player.App
import by.tigre.music.player.MainActivity
import by.tigre.music.player.core.presentation.backgound_player.view.BackgroundComponent
import by.tigre.music.player.core.presentation.backgound_player.view.BackgroundPlayerView
import kotlinx.coroutines.cancel

class BackgroundService : Service() {
    private val component: BackgroundComponent by lazy {
        BackgroundComponent.Impl(dependency = (application as App).graph)
    }
    private val view: BackgroundPlayerView by lazy {
        BackgroundPlayerView(
            service = this,
            component = component,
            onIntentProvider = { Intent(this, MainActivity::class.java) }
        )
    }

    override fun onCreate() {
        super.onCreate()
        view.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        component.cancel()
        view.destroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(p0: Intent?): IBinder? = null

}
