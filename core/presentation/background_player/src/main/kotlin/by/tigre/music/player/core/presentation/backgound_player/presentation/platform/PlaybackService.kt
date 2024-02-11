package by.tigre.music.player.core.presentation.backgound_player.presentation.platform

import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import by.tigre.music.player.core.presentation.backgound_player.di.PlayerBackgroundDependency
import by.tigre.music.player.core.presentation.backgound_player.presentation.component.BackgroundComponent
import by.tigre.music.player.core.presentation.backgound_player.presentation.view.BackgroundPlayerView
import by.tigre.music.player.logger.Log
import kotlinx.coroutines.cancel

@OptIn(UnstableApi::class)
abstract class PlaybackService : MediaSessionService() {

    private val component: BackgroundComponent by lazy {
        BackgroundComponent.Impl(dependency = onProviderDependency())
    }
    private val view: BackgroundPlayerView by lazy {
        BackgroundPlayerView(
            service = this,
            component = component,
            onIntentProvider = ::onProviderMainIntent
        )
    }

    override fun onCreate() {
        Log.i("PlaybackService") { "onCreate" }
        super.onCreate()
        view.onCreate()

        setMediaNotificationProvider(view.mediaNotificationProvider())
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, session.player.playWhenReady) // hack for "stop playing in background"
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = view.onGetSession()

    override fun onDestroy() {
        super.onDestroy()
        component.cancel()
        view.destroy()
        Log.i("PlaybackService") { "onDestroy" }
    }

    abstract fun onProviderMainIntent(): Intent
    abstract fun onProviderDependency(): PlayerBackgroundDependency

}