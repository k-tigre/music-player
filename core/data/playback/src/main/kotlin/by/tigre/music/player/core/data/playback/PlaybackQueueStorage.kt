package by.tigre.music.player.core.data.playback

import by.tigre.music.player.core.data.entiry.playback.Playlist
import by.tigre.music.player.core.data.entiry.playback.SongItem
import by.tigre.music.player.tools.entiry.common.Optional
import kotlinx.coroutines.flow.StateFlow

interface PlaybackQueueStorage {
    val currentPlaylist: StateFlow<Optional<Playlist>>

    fun playSongs(items: List<SongItem>)
    fun playPlaylist(item: Playlist)
    fun setPlayingIndex(index: Int)
}
