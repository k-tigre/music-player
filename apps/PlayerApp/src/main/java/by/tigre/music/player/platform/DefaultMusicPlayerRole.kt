package by.tigre.music.player.platform

import android.app.DownloadManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object DefaultMusicPlayerRole {

    private const val ROLE_MUSIC = "android.app.role.MUSIC"

    fun canRequestRole(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleAvailable(ROLE_MUSIC) && !roleManager.isRoleHeld(ROLE_MUSIC)
    }

    fun isHeld(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleAvailable(ROLE_MUSIC) && roleManager.isRoleHeld(ROLE_MUSIC)
    }

    fun createRequestIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
        if (!roleManager.isRoleAvailable(ROLE_MUSIC) || roleManager.isRoleHeld(ROLE_MUSIC)) return null
        return roleManager.createRequestRoleIntent(ROLE_MUSIC)
    }

    fun createOpenDownloadsIntent(context: Context): Intent? =
        Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).takeIf { intent ->
            intent.resolveActivity(context.packageManager) != null
        }

    fun createAppDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
}
