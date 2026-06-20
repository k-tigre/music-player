package by.tigre.music.player.core.data.storage.playback_queue

import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.flow.Flow

interface PlaybackQueueStorage {
    val currentQueue: Flow<List<QueueItem>>
    val shuffleEnabled: Flow<Boolean>
    val repeatMode: Flow<RepeatMode>

    /** Sync snapshot for startup UI; updated whenever the queue table changes. */
    fun hasPersistedQueueItems(): Boolean

    suspend fun playSongs(items: List<Song.Id>)
    suspend fun addSongs(items: List<Song.Id>)
    suspend fun setShuffleEnabled(enabled: Boolean)
    suspend fun setRepeatMode(mode: RepeatMode)

    suspend fun playNext()
    suspend fun playPrev()
    suspend fun playSongInQueue(queueId: Long)
    suspend fun removeSongsByIds(ids: List<Song.Id>)

    data class QueueItem(val id: Long, val songsId: Song.Id, val state: State) {
        enum class State {
            Pending, Playing, Finish
        }
    }

    enum class RepeatMode {
        Off, All, One
    }
}
