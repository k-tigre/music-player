package by.tigre.music.player.tools.platform.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

fun Context.registerBroadcastReceiver(
    receiver: BroadcastReceiver?,
    filter: IntentFilter,
    receiverName: String
): Intent? = try {
    registerReceiver(receiver, filter)
} catch (e: Exception) {
    Log.w(receiverName, "Can't register receiver: $receiverName. Exception: ${e.message}")
    null
}

fun Context.unregisterBroadcastReceiver(receiver: BroadcastReceiver, receiverName: String) =
    try {
        unregisterReceiver(receiver)
    } catch (e: Exception) {
        Log.w(receiverName, "Can't unregister receiver: $receiverName. Exception: ${e.message}")
    }

fun Context.getNotificationManager(): NotificationManager =
    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
