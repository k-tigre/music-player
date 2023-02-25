package by.tigre.music.player.core.platform.permission.di

import android.content.Context
import by.tigre.music.player.core.platform.permission.PermissionsHelper

interface PermissionsDependency {
    val permissionsHelper: PermissionsHelper

    class Impl(context: Context) : PermissionsDependency {
        override val permissionsHelper: PermissionsHelper by lazy { PermissionsHelper.Impl(context) }
    }
}