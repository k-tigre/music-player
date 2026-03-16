package by.tigre.music.player.core.platform.permission.di

import android.content.Context
import by.tigre.music.player.core.platform.permission.AndroidPermissionsHelper
import by.tigre.music.player.core.platform.permission.PermissionsHelper

class AndroidPermissionsDependency(context: Context) : PermissionsDependency {
    override val permissionsHelper: PermissionsHelper by lazy { AndroidPermissionsHelper(context) }
}
