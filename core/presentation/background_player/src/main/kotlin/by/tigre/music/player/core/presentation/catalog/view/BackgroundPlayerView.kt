package by.tigre.music.player.core.presentation.catalog.view

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.support.v4.media.session.MediaSessionCompat
import by.tigre.music.player.tools.platform.utils.getNotificationManager
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

class BackgroundPlayerView(
    private val service: Service,
    private val component: BackgroundComponent,
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val notificationManager = service.getNotificationManager()


    private val MEDIA_SESSION_TAG = "MEDIA_SESSION_TAG"
    private val playerNotificationManager: PlayerNotificationManager
    private val mediaSessionCompat: MediaSessionCompat
    private val mediaSessionConnector: MediaSessionConnector

    init {
        val nc = NotificationChannel(NOTIFICATION_CHANEL_ID, "playback", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(nc)

        val listener = object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                if (dismissedByUser.not()) {
                    service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
                }
            }

            override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                service.startForeground(notificationId, notification)
            }
        }

        val mediaDescription = object : PlayerNotificationManager.MediaDescriptionAdapter {
            // FIXME: intent to main activity
            private val pendingIntent =
                PendingIntent.getActivity(service, 0, Intent("by.tigre.player.PLAYBACK_ACTION_RESTORE"), PendingIntent.FLAG_MUTABLE)

            override fun getCurrentContentTitle(player: Player): String = component.currentSong.value?.name ?: ""

            override fun createCurrentContentIntent(player: Player): PendingIntent? = pendingIntent

            override fun getCurrentContentText(player: Player): String? = component.currentSong.value?.let { "${it.artist}/${it.album}" }

            override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                return null
            }

            override fun getCurrentSubText(player: Player): CharSequence? = component.currentSong.value?.let { "${it.artist}/${it.album}" }
        }

        playerNotificationManager = PlayerNotificationManager.Builder(service, SERVICE_NOTIFICATION_ID, NOTIFICATION_CHANEL_ID)
            .setMediaDescriptionAdapter(mediaDescription)
            .setNotificationListener(listener)
            .build()
            .apply {
                setUseNextAction(true)
                setUseNextActionInCompactView(true)
                setUsePlayPauseActions(true)
                setUseFastForwardAction(false)
                setUsePreviousActionInCompactView(false)
                setUsePreviousAction(false)
                setUseRewindAction(false)

            }

        mediaSessionCompat = MediaSessionCompat(service, MEDIA_SESSION_TAG)
        playerNotificationManager.setMediaSessionToken(mediaSessionCompat.sessionToken)

        mediaSessionConnector = MediaSessionConnector(mediaSessionCompat)
    }


    fun onCreate() {
        mediaSessionCompat.isActive = true
        playerNotificationManager.setPlayer(component.getPlayer().player)
        mediaSessionConnector.setPlayer(component.getPlayer().player)
    }

    fun destroy() {
        scope.cancel()
    }


    companion object {
        private const val SERVICE_NOTIFICATION_ID = 101
        private const val NOTIFICATION_CHANEL_ID = "playback_01"
    }
}
