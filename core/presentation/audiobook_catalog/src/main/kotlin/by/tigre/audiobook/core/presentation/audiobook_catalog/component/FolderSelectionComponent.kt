package by.tigre.audiobook.core.presentation.audiobook_catalog.component

import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.entity.catalog.FolderSource
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogDependency
import by.tigre.audiobook.core.presentation.audiobook_catalog.navigation.AudiobookCatalogNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.presentation.base.ScreenContentStateDelegate
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface FolderSelectionComponent {

    val screenState: StateFlow<ScreenContentState<List<FolderSource>>>

    fun onFolderSelected(uri: String, name: String)
    fun onRemoveFolder(id: FolderSource.Id)
    fun onNavigateToBooks()
    fun retry()

    class Impl(
        context: BaseComponentContext,
        dependency: AudiobookCatalogDependency,
        private val navigator: AudiobookCatalogNavigator
    ) : FolderSelectionComponent, BaseComponentContext by context {

        private val catalogSource: AudiobookCatalogSource = dependency.audiobookCatalogSource

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = { catalogSource.folderSources },
            mapDataToState = { folders ->
                ScreenContentState.Content(folders)
            }
        )

        override val screenState: StateFlow<ScreenContentState<List<FolderSource>>> = stateDelegate.screenState

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

        override fun onNavigateToBooks() {
            navigator.showBookList()
        }

        override fun retry() {
            stateDelegate.reload()
        }
    }
}
