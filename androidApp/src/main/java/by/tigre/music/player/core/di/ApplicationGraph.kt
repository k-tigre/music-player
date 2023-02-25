package by.tigre.music.player.core.di

import android.content.Context
import by.tigre.music.player.core.data.storage.preferences.di.PreferencesDependency
import by.tigre.music.player.core.platform.permission.di.PermissionsDependency
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency

class ApplicationGraph(
    catalogDependency: CatalogDependency
) : CatalogDependency by catalogDependency {

    companion object {
        fun create(context: Context): ApplicationGraph {
            val preferencesDependency = PreferencesDependency.Impl(context)
            val permissionsDependency = PermissionsDependency.Impl(context)
            val catalogDependency = CatalogDependency.Impl(context, permissionsDependency, preferencesDependency)

            return ApplicationGraph(catalogDependency)
        }
    }
}
