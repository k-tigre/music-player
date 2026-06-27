package by.tigre.music.player.core.presentation.playlist.library.component

import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.presentation.ScreenContentState.Content
import by.tigre.media.platform.presentation.ScreenContentStateDelegate
import by.tigre.music.player.core.entiry.playlist.Playlist
import by.tigre.music.player.core.entiry.playlist.PlaylistTrackEntry
import by.tigre.music.player.core.presentation.playlist.library.di.PlaylistsDependency
import by.tigre.music.player.core.presentation.playlist.library.navigation.PlaylistsNavigator
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface PlaylistDetailComponent {
    val playlistId: Playlist.Id
    val playlistName: StateFlow<String>
    val screenState: StateFlow<ScreenContentState<List<PlaylistTrackEntry>>>
    val messages: SharedFlow<Message>

    fun retry()
    fun onBackClicked()
    fun onAddTracksClicked()
    fun onPlayAll()
    fun onAddAllToQueue()
    fun onPlayTrack(track: PlaylistTrackEntry)
    fun onAddTrackToQueue(track: PlaylistTrackEntry)
    fun onMoveTrackUp(entryId: Long)
    fun onMoveTrackDown(entryId: Long)
    fun onMoveTrackToTop(entryId: Long)
    fun onMoveTrackToBottom(entryId: Long)
    fun onTracksReordered(entryIdsInOrder: List<Long>)
    fun onRemoveTrack(track: PlaylistTrackEntry)
    fun onRename(name: String)
    fun onDeletePlaylist()
    fun onOpenArtist(track: PlaylistTrackEntry)
    fun onOpenAlbum(track: PlaylistTrackEntry)

    sealed interface Message {
        data object NoPlayableTracks : Message
    }

    class Impl(
        context: BaseComponentContext,
        dependency: PlaylistsDependency,
        private val navigator: PlaylistsNavigator,
        override val playlistId: Playlist.Id,
    ) : PlaylistDetailComponent, BaseComponentContext by context {

        private val playlistRepository = dependency.playlistRepository
        private val playbackController = dependency.playbackController
        private val eventAnalytics = dependency.eventAnalytics

        override val playlistName: StateFlow<String> = playlistRepository.allPlaylists
            .map { playlists ->
                playlists.firstOrNull { it.id == playlistId }?.name.orEmpty()
            }
            .stateIn(
                scope = this,
                started = SharingStarted.WhileSubscribed(),
                initialValue = ""
            )

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = { playlistRepository.tracks(playlistId) },
            mapDataToState = { Content(it) },
        )

        override val screenState: StateFlow<ScreenContentState<List<PlaylistTrackEntry>>> = stateDelegate.screenState

        private val _messages = MutableSharedFlow<Message>(extraBufferCapacity = 1)
        override val messages: SharedFlow<Message> = _messages.asSharedFlow()

        override fun retry() {
            stateDelegate.reload()
        }

        override fun onBackClicked() {
            navigator.showPreviousScreen()
        }

        override fun onAddTracksClicked() {
            navigator.openCatalog()
        }

        override fun onPlayAll() {
            launch {
                val ids = playlistRepository.resolvePlayableSongIds(playlistId)
                if (ids.isEmpty()) {
                    _messages.emit(Message.NoPlayableTracks)
                    return@launch
                }
                eventAnalytics.trackEvent(MusicEvents.Action.PlaylistPlayAll)
                playbackController.playSongs(ids)
                navigator.openQueue()
            }
        }

        override fun onAddAllToQueue() {
            launch {
                val ids = playlistRepository.resolvePlayableSongIds(playlistId)
                if (ids.isEmpty()) {
                    _messages.emit(Message.NoPlayableTracks)
                    return@launch
                }
                playbackController.addSongsToPlay(ids)
            }
        }

        override fun onPlayTrack(track: PlaylistTrackEntry) {
            track.song?.let { playbackController.playSong(it.id) }
        }

        override fun onAddTrackToQueue(track: PlaylistTrackEntry) {
            track.song?.let { playbackController.addSongToPlay(it.id) }
        }

        override fun onMoveTrackUp(entryId: Long) {
            moveTrack(entryId = entryId, offset = -1)
        }

        override fun onMoveTrackDown(entryId: Long) {
            moveTrack(entryId = entryId, offset = 1)
        }

        override fun onMoveTrackToTop(entryId: Long) {
            moveTrackToIndex(entryId = entryId, targetIndex = 0)
        }

        override fun onMoveTrackToBottom(entryId: Long) {
            val tracks = (screenState.value as? Content)?.value.orEmpty()
            if (tracks.isEmpty()) return
            moveTrackToIndex(entryId = entryId, targetIndex = tracks.lastIndex)
        }

        override fun onTracksReordered(entryIdsInOrder: List<Long>) {
            val currentOrder = (screenState.value as? Content)?.value.orEmpty().map { it.entryId }
            if (entryIdsInOrder == currentOrder) return

            launch {
                playlistRepository.reorderTracks(
                    entryIdsInOrder.mapIndexed { index, entryId ->
                        entryId to index
                    }
                )
            }
        }

        override fun onRemoveTrack(track: PlaylistTrackEntry) {
            launch {
                playlistRepository.removeTrack(track.entryId)
            }
        }

        private fun moveTrack(entryId: Long, offset: Int) {
            val tracks = (screenState.value as? Content)?.value.orEmpty()
            if (tracks.size < 2) return

            val sourceIndex = tracks.indexOfFirst { it.entryId == entryId }
            if (sourceIndex == -1) return

            val targetIndex = sourceIndex + offset
            if (targetIndex !in tracks.indices) return

            moveTrackToIndex(entryId = entryId, targetIndex = targetIndex)
        }

        private fun moveTrackToIndex(entryId: Long, targetIndex: Int) {
            val tracks = (screenState.value as? Content)?.value.orEmpty()
            if (tracks.size < 2) return

            val sourceIndex = tracks.indexOfFirst { it.entryId == entryId }
            if (sourceIndex == -1 || sourceIndex == targetIndex) return

            val reorderedTracks = tracks.toMutableList().apply {
                add(targetIndex, removeAt(sourceIndex))
            }

            launch {
                playlistRepository.reorderTracks(
                    reorderedTracks.mapIndexed { index, track ->
                        track.entryId to index
                    }
                )
            }
        }

        override fun onRename(name: String) {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) return
            launch {
                if (playlistRepository.isNameTaken(trimmedName, excludeId = playlistId)) {
                    return@launch
                }
                playlistRepository.renamePlaylist(playlistId, trimmedName)
            }
        }

        override fun onDeletePlaylist() {
            launch {
                playlistRepository.deletePlaylist(playlistId)
                navigator.showPreviousScreen()
            }
        }

        override fun onOpenArtist(track: PlaylistTrackEntry) {
            val song = track.song ?: return
            navigator.openArtist(song.artistId)
        }

        override fun onOpenAlbum(track: PlaylistTrackEntry) {
            val song = track.song ?: return
            navigator.openAlbum(song.artistId, song.albumId)
        }
    }
}
