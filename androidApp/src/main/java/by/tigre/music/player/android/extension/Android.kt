package by.tigre.music.player.android.extension

import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

private fun Context.getActivityManager(): ActivityManager =
    getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

inline fun <reified T : Activity> Context.start(noinline configure: (Intent.() -> Unit)? = null) {
    startActivity(Intent(this, T::class.java).apply { configure?.invoke(this) })
}

inline fun <reified T : AppCompatActivity> Activity.startForResult(
    requestCode: Int,
    noinline configure: (Intent.() -> Unit)? = null
) {
    startActivityForResult(Intent(this, T::class.java).apply { configure?.invoke(this) }, requestCode, null)
}


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
