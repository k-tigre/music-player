package by.tigre.music.player.core.data.storage.playback_queue.impl

import by.tigre.music.player.core.data.storage.music.DatabaseMusic
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage.QueueItem
import by.tigre.music.player.core.entiry.catalog.Song
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

class PlaybackQueueStorageImpl(private val database: DatabaseMusic, scope: CoroutineScope) : PlaybackQueueStorage {

    override val currentQueue: Flow<List<QueueItem>> =
        database.queueQueries.selectAll(
            limit = 10000,
            mapper = { id: Long, status: QueueItem.State, songId: Long ->
                QueueItem(id = id, songsId = Song.Id(songId), status)
            }
        ).asFlow().mapToList()
            .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    override fun playSongs(items: List<Song.Id>) {
        database.queueQueries.transaction {
            database.queueQueries.deleteAll()
            items.forEach { id ->
                database.queueQueries.insertNew(id.value)
            }
            items.firstOrNull()?.let {
                database.queueQueries.updateStatusBySongId(status = QueueItem.State.Playing, song_id = it.value)
            }
        }
    }

    override fun addSongs(items: List<Song.Id>) {
        database.queueQueries.transaction {
            items.forEach { id ->
                database.queueQueries.insertNew(id.value)
            }
        }
    }

    override fun playQueue(queue: List<QueueItem>) {
        database.queueQueries.transaction {
            database.queueQueries.deleteAll()
            queue.forEach { item ->
                database.queueQueries.insertNewWithStatus(
                    song_id = item.songsId.value,
                    status = item.state
                )
            }
        }
    }

    override fun updateSongStates(finishedId: Long?, playingId: Long, pendingId: Long?) {
        database.queueQueries.transaction {
            finishedId?.let { database.queueQueries.updateStatus(status = QueueItem.State.Finish, id = it) }
            pendingId?.let { database.queueQueries.updateStatus(status = QueueItem.State.Pending, id = it) }
            database.queueQueries.updateStatus(status = QueueItem.State.Playing, id = playingId)
        }
    }
}
