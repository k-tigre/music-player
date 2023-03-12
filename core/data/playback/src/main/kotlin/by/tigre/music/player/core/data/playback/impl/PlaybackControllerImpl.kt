package by.tigre.music.player.core.data.playback.impl

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playback.MediaItemWrapperProvider
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.tools.coroutines.CoreScope
import by.tigre.music.player.tools.coroutines.extensions.log
import by.tigre.music.player.tools.coroutines.extensions.withLatestFrom
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class PlaybackControllerImpl(
    private val storage: PlaybackQueueStorage,
    private val catalog: CatalogSource,
    override val player: PlaybackPlayer,
    private val mediaItemWrapperProvider: MediaItemWrapperProvider,
    private val scope: CoreScope
) : PlaybackController {

    private val isPlaying = MutableStateFlow(false)
    private val action = MutableSharedFlow<Action>(extraBufferCapacity = 1)
    override val currentItem: StateFlow<Song?> = storage.currentQueue
        .map { queue ->
            queue.firstOrNull { it.state == PlaybackQueueStorage.QueueItem.State.Playing }?.let {
                catalog.getSongById(id = it.songsId)
            }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(), initialValue = null)

    init {
        scope.launch {
            action.withLatestFrom(storage.currentQueue)
                .collect { (action, queue) ->
                    when (action) {
                        is Action.PlayNewSongs -> {
                            storage.playSongs(action.items)
                        }

                        is Action.PlayAlbum -> {
                            storage.playSongs(catalog.getSongsByAlbum(action.album.id))
                        }

                        Action.PlayNext -> {
                            val next = queue.firstOrNull { it.state == PlaybackQueueStorage.QueueItem.State.Pending }?.id
                            if (next != null) {
                                val current = queue.firstOrNull { it.state == PlaybackQueueStorage.QueueItem.State.Playing }?.id ?: -1
                                storage.setSongPlayed(id = current, nextId = next)
                            } else {
                                storage.playQueue(queue)
                            }
                        }

                        Action.PlayPrev -> {
                            // TODO
                        }
                    }
                }
        }

        scope.launch {
            player.state
                .filter { it == PlaybackPlayer.State.Ended }
                .collect { playNext() }
        }

        scope.launch {
            currentItem
                .collect { item ->
                    if (item != null) {
                        player.setMediaItem(mediaItemWrapperProvider.songToMediaItem(item), 0)
                    } else {
                        player.stop()
                    }
                }
        }

        scope.launch {
            isPlaying.collect {
                if (it) player.resume() else player.pause()
            }
        }
    }

    override fun playNext() {
        action.tryEmit(Action.PlayNext)
    }

    override fun playPrev() {
        action.tryEmit(Action.PlayPrev)
    }

    override fun pause() {
        isPlaying.tryEmit(false)
    }

    override fun resume() {
        isPlaying.tryEmit(true)
    }

    override fun playSongs(items: List<Song>, startPosition: Int) {
        action.tryEmit(Action.PlayNewSongs(items, startPosition))
        resume()
    }

    override fun playAlbum(album: Album) {
        action.tryEmit(Action.PlayAlbum(album))
        resume()
    }

    //    override fun playSongs(item: Playlist) {
//        action.tryEmit(Action.PlayNewPlaylist(item))
//    }

    override fun stop() {
        scope.launch { player.stop() }
    }

    private sealed interface Action {
        object PlayNext : Action
        object PlayPrev : Action
//        object Pause : Action
//        object Resume : Action
//        object Stop : Action
        data class PlayNewSongs(val items: List<Song>, val startPosition: Int) : Action
        data class PlayAlbum(val album: Album) : Action
    }
}
