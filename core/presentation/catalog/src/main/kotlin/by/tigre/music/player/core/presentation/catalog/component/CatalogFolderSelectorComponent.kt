package by.tigre.music.player.core.presentation.catalog.component

import android.net.Uri
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.platform.permission.PermissionsHelper
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.music.player.core.presentation.catalog.navigation.CatalogNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface CatalogFolderSelectorComponent {

    val isPermissionGranted: StateFlow<Boolean>
    val isFolderSelected: StateFlow<Boolean>
    val inProgress: StateFlow<Boolean>
    val onSelectError: Flow<Unit>

    fun selectNewRootFolder(path: Uri)
    fun launchPermissionRequest()

    class Impl(
        context: BaseComponentContext,
        dependency: CatalogDependency,
        private val navigator: CatalogNavigator,
    ) : CatalogFolderSelectorComponent, BaseComponentContext by context {

        private val catalogSource: CatalogSource = dependency.catalogSource
        private val permission: PermissionsHelper = dependency.permissionHelper

        override val onSelectError = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        override val inProgress = MutableStateFlow(false)
        override val isPermissionGranted = MutableStateFlow(permission.isGranted(PermissionsHelper.Permission.ReadAudioFiles))
        override val isFolderSelected = catalogSource.rootCatalogFolderSelected.map { it.isNullOrBlank().not() }
            .onEach { inProgress.emit(false) }
            .stateIn(this, SharingStarted.Eagerly, initialValue = false)

        init {
            launch {
                combine(isFolderSelected, isPermissionGranted) { hasFolder, hasPermission -> hasFolder && hasPermission }
                    .filter { it }
                    .take(1)
                    .collect {
                        withContext(Dispatchers.Main) {
                            navigator.showArtists()
                        }
                    }
            }
        }

        override fun launchPermissionRequest() {

        }

        override fun selectNewRootFolder(path: Uri) {
            if (catalogSource.setRootFolder(path).not()) {
                onSelectError.tryEmit(Unit)
            }
        }
    }
}
