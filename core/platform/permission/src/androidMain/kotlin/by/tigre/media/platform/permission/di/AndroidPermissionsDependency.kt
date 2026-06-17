package by.tigre.media.platform.permission.di

import android.content.Context
import by.tigre.media.platform.permission.AndroidPermissionsHelper
import by.tigre.media.platform.permission.PermissionsHelper

class AndroidPermissionsDependency(context: Context) : PermissionsDependency {
    override val permissionsHelper: PermissionsHelper by lazy { AndroidPermissionsHelper(context) }
}
