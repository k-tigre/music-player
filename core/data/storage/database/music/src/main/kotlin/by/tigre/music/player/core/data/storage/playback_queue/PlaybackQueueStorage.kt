package by.tigre.music.player.core.data.storage.playback_queue

import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.flow.Flow

interface PlaybackQueueStorage {
    val currentQueue: Flow<List<QueueItem>>

    fun playSongs(items: List<Song.Id>)
    fun playQueue(queue: List<QueueItem>)
    fun addSongs(items: List<Song.Id>)
    fun updateSongStates(finishedId: Long?, playingId: Long, pendingId: Long?)

    data class QueueItem(val id: Long, val songsId: Song.Id, val state: State) {
        enum class State {
            Pending, Playing, Finish
        }
    }
}
