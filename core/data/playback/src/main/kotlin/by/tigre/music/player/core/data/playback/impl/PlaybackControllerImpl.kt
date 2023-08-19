package by.tigre.music.player.core.data.playback.impl

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playback.MediaItemWrapperProvider
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.entiry.playback.SongInQueueItem
import by.tigre.music.player.tools.coroutines.CoreScope
import by.tigre.music.player.tools.coroutines.extensions.withLatestFrom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
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

    override val currentQueue: Flow<List<SongInQueueItem>> =
        combine(
            storage.currentQueue
                .map {
                    it.map(PlaybackQueueStorage.QueueItem::songsId)
                }
                .distinctUntilChanged()
                .map { ids -> catalog.getSongsByIds(ids) },
            storage.currentQueue.map { it.find { item -> item.state == PlaybackQueueStorage.QueueItem.State.Playing } }
        ) { songs, playingItem ->
            songs.map { song ->
                SongInQueueItem(
                    song,
                    isPlaying = song.id == playingItem?.songsId
                )
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(), initialValue = emptyList())

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

    override fun stop() {
        scope.launch { player.stop() }
    }

    private sealed interface Action {
        data object PlayNext : Action
        data object PlayPrev : Action

        data class PlayNewSongs(val items: List<Song>, val startPosition: Int) : Action
        data class PlayAlbum(val album: Album) : Action
    }
}
