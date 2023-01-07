package by.tigre.music.player.android.presentation.background

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.cancel

class BackgroundService : Service() {
    private var model: BackgroundPresenter? = null

    override fun onCreate() {
        super.onCreate()
        model = BackgroundPresenter.Impl(
            view = BackgroundView.Impl(this),
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        model?.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(p0: Intent?): IBinder? = null

}
