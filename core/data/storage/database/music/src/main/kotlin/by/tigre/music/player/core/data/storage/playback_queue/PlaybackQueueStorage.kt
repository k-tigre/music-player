package by.tigre.music.player.core.data.storage.playback_queue

import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.flow.Flow

interface PlaybackQueueStorage {
    val currentQueue: Flow<List<QueueItem>>

    fun playSongs(items: List<Song>)
    fun playQueue(queue: List<QueueItem>)
    fun setSongPlayed(id: Long, nextId: Long?)

    data class QueueItem(val id: Long, val songsId: Long, val state: State) {
        enum class State {
            Pending, Playing, Finish
        }
    }
}
