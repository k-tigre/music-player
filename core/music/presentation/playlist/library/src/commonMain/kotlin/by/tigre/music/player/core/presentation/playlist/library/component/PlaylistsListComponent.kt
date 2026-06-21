package by.tigre.music.player.core.presentation.playlist.library.component

import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.presentation.ScreenContentState.Content
import by.tigre.media.platform.presentation.ScreenContentStateDelegate
import by.tigre.music.player.core.entiry.playlist.Playlist
import by.tigre.music.player.core.presentation.playlist.library.di.PlaylistsDependency
import by.tigre.music.player.core.presentation.playlist.library.navigation.PlaylistsNavigator
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface PlaylistsListComponent {

    val screenState: StateFlow<ScreenContentState<List<Playlist>>>
    val dialogState: StateFlow<PlaylistsDialogState?>

    fun retry()
    fun onCreateClicked()
    fun onCreateConfirmed(name: String)
    fun onPlaylistClicked(playlist: Playlist)
    fun onRenameClicked(playlist: Playlist)
    fun onRenameConfirmed(name: String)
    fun onDeleteClicked(playlist: Playlist)
    fun onDeleteConfirmed()
    fun dismissDialog()

    sealed interface PlaylistsDialogState {
        data object Create : PlaylistsDialogState
        data class Rename(val playlist: Playlist) : PlaylistsDialogState
        data class Delete(val playlist: Playlist) : PlaylistsDialogState
    }

    class Impl(
        context: BaseComponentContext,
        dependency: PlaylistsDependency,
        private val navigator: PlaylistsNavigator,
    ) : PlaylistsListComponent, BaseComponentContext by context {

        private val playlistRepository = dependency.playlistRepository
        private val eventAnalytics = dependency.eventAnalytics

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = { playlistRepository.allPlaylists },
            mapDataToState = { Content(it) },
        )

        override val screenState: StateFlow<ScreenContentState<List<Playlist>>> = stateDelegate.screenState

        private val _dialogState = MutableStateFlow<PlaylistsDialogState?>(null)
        override val dialogState: StateFlow<PlaylistsDialogState?> = _dialogState.asStateFlow()

        override fun retry() {
            stateDelegate.reload()
        }

        override fun onCreateClicked() {
            _dialogState.value = PlaylistsDialogState.Create
        }

        override fun onCreateConfirmed(name: String) {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) return
            launch {
                val id = playlistRepository.createPlaylist(trimmedName)
                eventAnalytics.trackEvent(MusicEvents.Action.PlaylistCreate)
                _dialogState.value = null
                navigator.openDetail(id)
            }
        }

        override fun onPlaylistClicked(playlist: Playlist) {
            navigator.openDetail(playlist.id)
        }

        override fun onRenameClicked(playlist: Playlist) {
            _dialogState.value = PlaylistsDialogState.Rename(playlist)
        }

        override fun onRenameConfirmed(name: String) {
            val dialog = _dialogState.value as? PlaylistsDialogState.Rename ?: return
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) return
            launch {
                playlistRepository.renamePlaylist(dialog.playlist.id, trimmedName)
                _dialogState.value = null
            }
        }

        override fun onDeleteClicked(playlist: Playlist) {
            _dialogState.value = PlaylistsDialogState.Delete(playlist)
        }

        override fun onDeleteConfirmed() {
            val dialog = _dialogState.value as? PlaylistsDialogState.Delete ?: return
            launch {
                playlistRepository.deletePlaylist(dialog.playlist.id)
                eventAnalytics.trackEvent(MusicEvents.Action.PlaylistDelete)
                _dialogState.value = null
            }
        }

        override fun dismissDialog() {
            _dialogState.value = null
        }
    }
}
