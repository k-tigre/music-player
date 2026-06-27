package by.tigre.music.player.core.data.playback

import by.tigre.media.platform.playback.PlaybackPlayer
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.entiry.playback.PlaybackInterruption
import by.tigre.music.player.core.entiry.playback.PlayableItem
import by.tigre.music.player.core.entiry.playback.QueueSession
import by.tigre.music.player.core.entiry.playback.SongInQueueItem
import by.tigre.music.player.core.entiry.playlist.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface ActivePlaybackSource {
    data object Session : ActivePlaybackSource
    data class Overlay(val item: PlayableItem.ExternalAudio) : ActivePlaybackSource
}

interface PlaybackController {
    val player: PlaybackPlayer
    val currentItem: StateFlow<Song?>
    val currentQueue: Flow<List<SongInQueueItem>>
    val shuffleEnabled: Flow<Boolean>
    val repeatMode: Flow<PlaybackQueueStorage.RepeatMode>
    /** Whether the playback engine is actively playing (not paused or idle). */
    val isPlaying: StateFlow<Boolean>
    val activePlaybackSource: StateFlow<ActivePlaybackSource>
    val interruption: StateFlow<PlaybackInterruption?>
    val nowPlayingOverlay: StateFlow<PlayableItem.ExternalAudio?>
    val queueSession: StateFlow<QueueSession>

    fun playNext()
    fun playPrev()
    fun pause()
    fun resume()
    fun stop()
    fun playSong(id: Song.Id)
    fun playSongs(ids: List<Song.Id>)
    fun playPlaylist(playlistId: Playlist.Id, name: String, songIds: List<Song.Id>)
    fun playSongInQueue(id: Long)
    fun playAlbum(albumId: Album.Id, artistId: Artist.Id)
    fun addAlbumToPlay(id: Album.Id, artistId: Artist.Id)
    fun addSongToPlay(id: Song.Id)
    fun addSongsToPlay(ids: List<Song.Id>)
    fun playArtist(id: Artist.Id)
    fun addArtistToPlay(id: Artist.Id)
    fun toggleShuffle()
    fun cycleRepeat()
    fun removeSongsFromQueue(ids: List<Song.Id>)
    fun removeFromQueue(queueEntryIds: List<Long>)
    fun reorderQueue(queueEntryIdsInOrder: List<Long>)
    suspend fun queueSongIdsInListOrder(): List<Song.Id>
    fun markPlaylistSaved()
    fun activatePlaylistSession(playlistId: Playlist.Id, name: String)
    fun playExternal(item: PlayableItem.ExternalAudio)
    fun resumeInterruptedSession()
}
