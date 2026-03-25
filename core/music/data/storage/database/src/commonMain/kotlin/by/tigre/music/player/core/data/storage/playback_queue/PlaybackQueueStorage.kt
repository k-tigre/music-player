package by.tigre.music.player.core.data.storage.playback_queue

import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.flow.Flow

interface PlaybackQueueStorage {
    val currentQueue: Flow<List<QueueItem>>
    val orderMode: Flow<OrderMode>

    suspend fun playSongs(items: List<Song.Id>)
    suspend fun addSongs(items: List<Song.Id>)
    suspend fun setOrderMode(mode: OrderMode)

    suspend fun playNext()
    suspend fun playPrev()
    suspend fun playSongInQueue(queueId: Long)

    data class QueueItem(val id: Long, val songsId: Song.Id, val state: State) {
        enum class State {
            Pending, Playing, Finish
        }
    }

    enum class OrderMode {
        Normal, Random
    }
}
