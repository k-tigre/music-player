package by.tigre.music.player.core.data.playback

import by.tigre.music.player.core.data.entiry.playback.Playlist
import by.tigre.music.player.core.data.entiry.playback.SongItem
import by.tigre.music.player.tools.coroutines.CoreScope
import by.tigre.music.player.tools.coroutines.extensions.withLatestFrom
import by.tigre.music.player.tools.entiry.common.Optional
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

interface PlaybackController {
    val currentItem: StateFlow<Optional<SongItem>>

    fun playNext()
    fun playPrev()
    fun pause()
    fun resume()
    fun stop()
    fun playSongs(items: List<SongItem>, startPosition: Int)
    fun playSongs(item: Playlist)

    class Impl(
        private val storage: PlaybackQueueStorage,
        private val player: PlaybackPlayer,
        private val mediaItemWrapperProvider: MediaItemWrapperProvider,
        private val scope: CoreScope
    ) : PlaybackController {

        private val action = MutableSharedFlow<Action>(extraBufferCapacity = 1)
        override val currentItem = MutableStateFlow(Optional.None)

        init {
            action.withLatestFrom(storage.currentPlaylist)
                .onEach { (action, playlist) ->
                    when (action) {
                        Action.Pause -> player.pause()
                        is Action.PlayNewPlaylist -> {
                            storage.playPlaylist(action.item)
                        }
                        is Action.PlayNewSongs -> {
                            storage.playSongs(action.items)
                        }
                        Action.PlayNext -> {
                            if (playlist is Optional.Some) {
                                val index = playlist.value.playingItemIndex
                                if (index < playlist.value.items.size - 2) {
                                    storage.setPlayingIndex(index + 1)
                                } else {
                                    // TODO
                                }
                            }
                        }
                        Action.PlayPrev -> {
                            if (playlist is Optional.Some) {
                                val index = playlist.value.playingItemIndex
                                if (index > 1) {
                                    storage.setPlayingIndex(index - 1)
                                } else {
                                    // TODO
                                }
                            }
                        }
                        Action.Resume -> player.resume()
                        Action.Stop -> player.stop()
                    }
                }
                .launchIn(scope)

            player.state
                .filter { it == PlaybackPlayer.State.Ended }
                .onEach { playNext() }
                .launchIn(scope)

            storage.currentPlaylist
                .onEach { playlist ->
                    playlist.process(
                        onSome = {
                            val song = it.items[it.playingItemIndex]
                            player.setMediaItem(mediaItemWrapperProvider.songToMediaItem(song), 0)
                        },
                        onNone = {
                            player.stop()
                        }
                    )
                }
                .launchIn(scope)
        }

        override fun playNext() {
            action.tryEmit(Action.PlayNext)
        }

        override fun playPrev() {
            action.tryEmit(Action.PlayPrev)
        }

        override fun pause() {
            action.tryEmit(Action.Pause)
        }

        override fun resume() {
            action.tryEmit(Action.Resume)
        }

        override fun playSongs(items: List<SongItem>, startPosition: Int) {
            action.tryEmit(Action.PlayNewSongs(items, startPosition))
        }

        override fun playSongs(item: Playlist) {
            action.tryEmit(Action.PlayNewPlaylist(item))
        }

        override fun stop() {
            action.tryEmit(Action.Stop)
        }
    }

    private sealed interface Action {
        object PlayNext : Action
        object PlayPrev : Action
        object Pause : Action
        object Resume : Action
        object Stop : Action
        data class PlayNewSongs(val items: List<SongItem>, val startPosition: Int) : Action
        data class PlayNewPlaylist(val item: Playlist) : Action
    }
}
