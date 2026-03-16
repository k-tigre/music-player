package by.tigre.music.player.core.platform.permission

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

class AndroidPermissionsHelper(private val context: Context) : PermissionsHelper {
    private val preference = context.getSharedPreferences("permission", Context.MODE_PRIVATE)

    override fun isGranted(permission: PermissionsHelper.Permission): Boolean {
        val key = permissionKey(permission)
        return ActivityCompat.checkSelfPermission(context, key) == PackageManager.PERMISSION_GRANTED
    }

    override fun isFirstTimeInfoShown(permission: PermissionsHelper.Permission): Boolean {
        val key = permissionKey(permission)
        return preference.getBoolean("${SHOWN_KEY}${key}", false)
    }

    override fun setFirstTimeInfoShown(permission: PermissionsHelper.Permission) {
        val key = permissionKey(permission)
        preference.edit().putBoolean("${SHOWN_KEY}${key}", true).apply()
    }

    override fun isPermissionRequested(permission: PermissionsHelper.Permission): Boolean {
        val key = permissionKey(permission)
        return preference.getBoolean("${REQUESTED_KEY}${key}", false)
    }

    override fun setPermissionRequested(permission: PermissionsHelper.Permission) {
        val key = permissionKey(permission)
        preference.edit().putBoolean("${REQUESTED_KEY}${key}", true).apply()
    }

    @SuppressLint("InlinedApi")
    override fun permissionKey(permission: PermissionsHelper.Permission): String = when (permission) {
        PermissionsHelper.Permission.ReadAudioFiles -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }
        PermissionsHelper.Permission.Notification -> Manifest.permission.POST_NOTIFICATIONS
    }

    private companion object {
        const val SHOWN_KEY = "first_info_"
        const val REQUESTED_KEY = "requested_"
    }
}
