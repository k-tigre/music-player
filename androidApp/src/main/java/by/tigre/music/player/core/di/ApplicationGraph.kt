package by.tigre.music.player.core.di

import android.content.Context
import by.tigre.music.player.core.data.catalog.di.CatalogModule
import by.tigre.music.player.core.data.playback.di.PlaybackModule
import by.tigre.music.player.core.data.storage.playback_queue.di.PlaybackQueueModule
import by.tigre.music.player.core.data.storage.preferences.di.PreferencesDependency
import by.tigre.music.player.core.platform.permission.di.PermissionsDependency
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.music.player.core.presentation.catalog.di.PlayerDependency
import by.tigre.music.player.tools.coroutines.CoroutineModule

class ApplicationGraph(
    playbackModule: PlaybackModule,
    catalogModule: CatalogModule
) : CatalogDependency,
    PlayerDependency,
    PlaybackModule by playbackModule,
    CatalogModule by catalogModule
{

    companion object {
        fun create(context: Context): ApplicationGraph {
//            val preferencesDependency = PreferencesDependency.Impl(context)
//            val permissionsDependency = PermissionsDependency.Impl(context)
            val catalogModule = CatalogModule.Impl(context)

            val coroutineModule = CoroutineModule.Impl()
            val playbackQueueModule = PlaybackQueueModule.Impl(context, scope = coroutineModule.scope)
            val playbackModule = PlaybackModule.Impl(context, coroutineModule, playbackQueueModule, catalogModule)

            return ApplicationGraph(playbackModule, catalogModule)
        }
    }
}
