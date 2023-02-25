package by.tigre.music.player.core.platform.permission

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

interface PermissionsHelper {

    enum class Permission(internal val key: String) {
        ReadAudioFiles(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        ),
        @SuppressLint("InlinedApi")
        Notification(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun isGranted(permission: Permission): Boolean

    fun isFirstTimeInfoShown(permission: Permission): Boolean
    fun setFirstTimeInfoShown(permission: Permission)

    fun isPermissionRequested(permission: Permission): Boolean
    fun setPermissionRequested(permission: Permission)

    class Impl(private val context: Context) : PermissionsHelper {
        private val preference = context.getSharedPreferences("permission", Context.MODE_PRIVATE)

        override fun isGranted(permission: Permission): Boolean =
            if (permission != Permission.Notification) {
                ActivityCompat.checkSelfPermission(context, permission.key) == PackageManager.PERMISSION_GRANTED
            } else {
                ActivityCompat.checkSelfPermission(context, permission.key) == PackageManager.PERMISSION_GRANTED
//                true // TODO handle notification permission on android 13
            }

        override fun isFirstTimeInfoShown(permission: Permission): Boolean =
            preference.getBoolean("${SHOWN_KEY}${permission.key}", false)

        override fun setFirstTimeInfoShown(permission: Permission) {
            preference.edit().putBoolean("${SHOWN_KEY}${permission.key}", true).apply()
        }

        override fun isPermissionRequested(permission: Permission): Boolean =
            preference.getBoolean("${REQUESTED_KEY}${permission.key}", false)

        override fun setPermissionRequested(permission: Permission) {
            preference.edit().putBoolean("${REQUESTED_KEY}${permission.key}", true).apply()
        }

        private companion object {
            const val SHOWN_KEY = "first_info_"
            const val REQUESTED_KEY = "requested_"
        }
    }
}
