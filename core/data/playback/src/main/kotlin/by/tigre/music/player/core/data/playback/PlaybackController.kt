package by.tigre.music.player.core.data.playback

import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.tools.entiry.common.Optional
import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {
    val player: PlaybackPlayer
    val currentItem: StateFlow<Song?>

    fun playNext()
    fun playPrev()
    fun pause()
    fun resume()
    fun stop()
    fun playSongs(items: List<Song>, startPosition: Int)
    fun playAlbum(album: Album)
}
