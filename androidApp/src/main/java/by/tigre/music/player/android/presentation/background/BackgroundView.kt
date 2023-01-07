package by.tigre.music.player.android.presentation.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.core.app.NotificationCompat.Action
import androidx.core.app.NotificationCompat.Builder
import by.tigre.music.player.android.MainActivity
import by.tigre.music.player.android.R
import by.tigre.music.player.android.extension.getNotificationManager
import by.tigre.music.player.android.extension.registerBroadcastReceiver
import by.tigre.music.player.android.extension.unregisterBroadcastReceiver
import by.tigre.music.player.android.presentation.background.BackgroundView.Action.RestoreScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow


interface BackgroundView {
    companion object {
        private const val SERVICE_NOTIFICATION_ID = 101
        private const val NOTIFICATION_CHANEL_ID = "playback_01"
    }

    val notificationAction: Flow<Action>

    fun onDestroy()
    fun onCreate()
    fun updateNotification(title: String, text: String, actions: List<Action>)

    enum class Action(val value: String, val title: String) {
        Stop("ACTION_STOP", "STOP"),
        RestoreScreen("ACTION_RESTORE_SCREEN", "");

        companion object {
            fun from(value: String?): Action? = values().find { value == it.value }
        }
    }

    class Impl(private val service: BackgroundService) : BackgroundView {
        private val notificationManager = service.getNotificationManager()
        private val requestCode = 11

        private val filter = IntentFilter().apply {
            Action.values().forEach { addAction(it.value) }
        }

        private fun Action.toPendingIntent(): PendingIntent {
            val flag = if (VERSION.SDK_INT >= VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return if (this == RestoreScreen) {
                val intent = Intent(service, MainActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

                PendingIntent.getActivity(service, requestCode, intent, flag)
            } else {
                PendingIntent.getBroadcast(service, requestCode, Intent(value), flag)
            }
        }

        private fun Action.toNotificationAction() = Action(
            R.drawable.notification_action, title, toPendingIntent()
        )

        private val actionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                Action.from(intent?.action)?.let { notificationAction.tryEmit(it) }
            }
        }

        init {
            if (VERSION.SDK_INT >= VERSION_CODES.O) {
                val nc = NotificationChannel(NOTIFICATION_CHANEL_ID, "playback", NotificationManager.IMPORTANCE_LOW)
                notificationManager.createNotificationChannel(nc)
            }
        }

        override val notificationAction = MutableSharedFlow<Action>(extraBufferCapacity = 1)

        override fun onCreate() {
            service.registerBroadcastReceiver(actionReceiver, filter, "notification")
            service.startForeground(
                SERVICE_NOTIFICATION_ID,
                buildNotification(
                    service,
                    title = "Test",
                    text = "Prosto test service",
                    actions = listOf(Action.Stop)
                )
            )
        }

        override fun updateNotification(title: String, text: String, actions: List<Action>) {
            kotlin.runCatching {
                notificationManager.notify(
                    SERVICE_NOTIFICATION_ID,
                    buildNotification(service, title, text, actions)
                )
            }
        }

        override fun onDestroy() {
            service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
            service.unregisterBroadcastReceiver(actionReceiver, "notification")
            service.stopSelf()
        }

        private fun buildNotification(
            context: Context,
            title: String? = null,
            text: String? = null,
            actions: List<Action>? = null,
        ) = Builder(context, NOTIFICATION_CHANEL_ID).apply {
            setOngoing(true)
            setSmallIcon(R.drawable.ic_android_black_24dp)
            setContentIntent(RestoreScreen.toPendingIntent())
            setContentTitle(title)
            setContentText(text)

            actions?.map { it.toNotificationAction() }?.forEach { addAction(it) }
        }.build()
    }
}
