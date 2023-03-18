package by.tigre.music.player.extension

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
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
