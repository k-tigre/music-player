package by.tigre.audiobook.core.presentation.audiobook_catalog.component

import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook.CatalogScanUi
import by.tigre.audiobook.core.data.audiobook.FolderSourceAccessHealth
import by.tigre.audiobook.core.entity.catalog.FolderSource
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogDependency
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.presentation.ScreenContentStateDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface FolderSelectionComponent {

    val screenState: StateFlow<ScreenContentState<List<FolderSource>>>
    val catalogScanUi: StateFlow<CatalogScanUi>
    val folderAccessHealth: StateFlow<Map<FolderSource.Id, FolderSourceAccessHealth>>

    fun refreshFolderAccessHealth()

    fun onFolderSelected(uri: String, name: String)
    fun onRemoveFolder(id: FolderSource.Id)
    fun onRescanFolders()
    fun onBack()
    fun retry()

    class Impl(
        context: BaseComponentContext,
        dependency: AudiobookCatalogDependency,
        private val navigator: AudiobookCatalogNavigator
    ) : FolderSelectionComponent, BaseComponentContext by context {

        private val catalogSource: AudiobookCatalogSource = dependency.audiobookCatalogSource

        private val _folderAccessHealth =
            MutableStateFlow<Map<FolderSource.Id, FolderSourceAccessHealth>>(emptyMap())

        init {
            launch {
                catalogSource.folderSources.collect {
                    runFolderHealthUpdate()
                }
            }
        }

        private suspend fun runFolderHealthUpdate() {
            val folders = catalogSource.getFolderSourcesList()
            _folderAccessHealth.value =
                folders.associate { it.id to catalogSource.diagnoseFolderAccess(it) }
        }

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = { catalogSource.folderSources },
            mapDataToState = { folders ->
                ScreenContentState.Content(folders)
            }
        )

        override val screenState: StateFlow<ScreenContentState<List<FolderSource>>> = stateDelegate.screenState
        override val catalogScanUi: StateFlow<CatalogScanUi> = catalogSource.catalogScanUi
        override val folderAccessHealth: StateFlow<Map<FolderSource.Id, FolderSourceAccessHealth>> =
            _folderAccessHealth.asStateFlow()

        override fun refreshFolderAccessHealth() {
            launch { runFolderHealthUpdate() }
        }

        override fun onFolderSelected(uri: String, name: String) {
            launch {
                catalogSource.addFolderAndScan(uri, name)
            }
        }

        override fun onRemoveFolder(id: FolderSource.Id) {
            launch {
                catalogSource.removeFolder(id)
            }
        }

        override fun onRescanFolders() {
            launch {
                catalogSource.rescanAllFolders()
            }
        }

        override fun onBack() {
            navigator.showPreviousScreen()
        }

        override fun retry() {
            stateDelegate.reload()
        }
    }
}
