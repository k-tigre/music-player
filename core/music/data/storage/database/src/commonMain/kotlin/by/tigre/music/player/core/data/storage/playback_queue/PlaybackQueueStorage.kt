package by.tigre.music.player.core.data.storage.playback_queue

import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.flow.Flow

interface PlaybackQueueStorage {
    val currentQueue: Flow<List<QueueItem>>
    val orderMode: Flow<OrderMode>

    suspend fun playSongs(items: List<Song.Id>)
    suspend fun playQueue(queue: List<QueueItem>)
    suspend fun addSongs(items: List<Song.Id>)
    suspend fun updateSongStates(finishedId: Long?, playingId: Long, pendingId: Long?)
    suspend fun resetStatusesInQueue()
    suspend fun resetAndPlayFirst()
    suspend fun resetAndPlayLast()

    suspend fun setOrderMode(mode: OrderMode)

    data class QueueItem(val id: Long, val songsId: Song.Id, val state: State) {
        enum class State {
            Pending, Playing, Finish
        }
    }

    enum class OrderMode {
        Normal, Random
    }
}
