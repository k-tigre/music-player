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
                QueueItem(id = id, songsId = songId, status)
            }
        ).asFlow().mapToList()
            .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    override fun playSongs(items: List<Song>) {
        database.queueQueries.transaction {
            database.queueQueries.deleteAll()
            items.forEach { (id) ->
                database.queueQueries.insertNew(id)
            }
            items.firstOrNull()?.let {
                database.queueQueries.updateStatusBySongId(status = QueueItem.State.Playing, song_id = it.id)
            }
        }
    }

    override fun playQueue(queue: List<QueueItem>) {
        database.queueQueries.transaction {
            database.queueQueries.deleteAll()
            queue.forEachIndexed { index, item ->
                database.queueQueries.insertNewWithStatus(
                    song_id = item.songsId,
                    status = if (index == 0) QueueItem.State.Playing else QueueItem.State.Pending
                )
            }
        }
    }

    override fun setSongPlayed(id: Long, nextId: Long?) {
        database.queueQueries.transaction {
            database.queueQueries.updateStatus(status = QueueItem.State.Finish, id = id)
            if (nextId != null)
                database.queueQueries.updateStatus(status = QueueItem.State.Playing, id = nextId)
        }
    }
}
