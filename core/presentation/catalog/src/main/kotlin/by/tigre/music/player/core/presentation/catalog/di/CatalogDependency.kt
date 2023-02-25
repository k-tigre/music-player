package by.tigre.music.player.core.presentation.catalog.di

import android.content.Context
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.catalog.android.DbHelper
import by.tigre.music.player.core.data.storage.preferences.Preferences
import by.tigre.music.player.core.data.storage.preferences.di.PreferencesDependency
import by.tigre.music.player.core.platform.permission.PermissionsHelper
import by.tigre.music.player.core.platform.permission.di.PermissionsDependency

interface CatalogDependency {
    val catalogSource: CatalogSource
    val permissionHelper: PermissionsHelper

    class Impl(
        context: Context,
        permissionsDependency: PermissionsDependency,
        preferencesDependency: PreferencesDependency
    ) : CatalogDependency {
        private val dbHelper: DbHelper by lazy { DbHelper.Impl(context) }
        override val catalogSource: CatalogSource by lazy { CatalogSource.Impl(dbHelper, preferencesDependency.preferences) }

        override val permissionHelper: PermissionsHelper by lazy { permissionsDependency.permissionsHelper }
    }
}
