package by.tigre.music.player.core.platform.permission.di

import by.tigre.music.player.core.platform.permission.PermissionsHelper

class DesktopPermissionsDependency : PermissionsDependency {
    override val permissionsHelper: PermissionsHelper by lazy {
        object : PermissionsHelper {
            override fun isGranted(permission: PermissionsHelper.Permission): Boolean = true
            override fun isFirstTimeInfoShown(permission: PermissionsHelper.Permission): Boolean = true
            override fun setFirstTimeInfoShown(permission: PermissionsHelper.Permission) = Unit
            override fun isPermissionRequested(permission: PermissionsHelper.Permission): Boolean = true
            override fun setPermissionRequested(permission: PermissionsHelper.Permission) = Unit
            override fun permissionKey(permission: PermissionsHelper.Permission): String = permission.name
        }
    }
}
