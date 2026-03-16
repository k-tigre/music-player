package by.tigre.music.player.core.platform.permission

interface PermissionsHelper {

    enum class Permission {
        ReadAudioFiles,
        Notification
    }

    fun isGranted(permission: Permission): Boolean

    fun isFirstTimeInfoShown(permission: Permission): Boolean
    fun setFirstTimeInfoShown(permission: Permission)

    fun isPermissionRequested(permission: Permission): Boolean
    fun setPermissionRequested(permission: Permission)

    fun permissionKey(permission: Permission): String
}
