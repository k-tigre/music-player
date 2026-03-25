package by.tigre.music.player.core.data.storage.playback_queue.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import by.tigre.music.player.core.data.storage.music.DatabaseMusic
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage.QueueItem
import by.tigre.music.player.core.data.storage.preferences.Preferences
import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

class PlaybackQueueStorageImpl(
    private val database: DatabaseMusic,
    private val preferences: Preferences,
    scope: CoroutineScope
) : PlaybackQueueStorage {

    private val queueMapper = { id: Long, status: QueueItem.State, songId: Long ->
        QueueItem(id = id, songsId = Song.Id(songId), status)
    }

    private val idMapper = { id: Long, _: QueueItem.State, _: Long -> id }

    override val orderMode = MutableStateFlow(getOrder())

    override val currentQueue: Flow<List<QueueItem>> = database.queueQueries.selectAllById(
        limit = 10000,
        mapper = queueMapper
    ).asFlow().mapToList(scope.coroutineContext)
        .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    override suspend fun playSongs(items: List<Song.Id>) {
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

    override suspend fun setOrderMode(mode: PlaybackQueueStorage.OrderMode) {
        if (mode == PlaybackQueueStorage.OrderMode.Random) {
            database.queueQueries.transaction {
                database.queueQueries.shuffleOrder()
            }
        }
        orderMode.emit(mode)
        saveOrder(mode)
    }

    override suspend fun addSongs(items: List<Song.Id>) {
        database.queueQueries.transaction {
            items.forEach { id ->
                database.queueQueries.insertNew(id.value)
            }
        }
    }

    override suspend fun playNext() {
        database.queueQueries.transaction {
            val queue = getPlaybackOrderedQueue()
            val next = queue.firstOrNull { it.state == QueueItem.State.Pending }
            if (next != null) {
                val current = queue.firstOrNull { it.state == QueueItem.State.Playing }
                current?.let { database.queueQueries.updateStatus(status = QueueItem.State.Finish, id = it.id) }
                database.queueQueries.updateStatus(status = QueueItem.State.Playing, id = next.id)
            } else {
                resetAndPlayFirst()
            }
        }
    }

    override suspend fun playPrev() {
        database.queueQueries.transaction {
            val queue = getPlaybackOrderedQueue()
            val prev = queue.lastOrNull { it.state == QueueItem.State.Finish }
            if (prev != null) {
                val current = queue.firstOrNull { it.state == QueueItem.State.Playing }
                current?.let { database.queueQueries.updateStatus(status = QueueItem.State.Pending, id = it.id) }
                database.queueQueries.updateStatus(status = QueueItem.State.Playing, id = prev.id)
            } else {
                resetAndPlayLast()
            }
        }
    }

    override suspend fun playSongInQueue(queueId: Long) {
        database.queueQueries.transaction {
            val queue = getPlaybackOrderedQueue()
            var found = false
            for (item in queue) {
                val newStatus = when {
                    item.id == queueId -> {
                        found = true
                        QueueItem.State.Playing
                    }
                    found -> QueueItem.State.Pending
                    else -> QueueItem.State.Finish
                }
                if (item.state != newStatus) {
                    database.queueQueries.updateStatus(status = newStatus, id = item.id)
                }
            }
        }
    }

    private fun getPlaybackOrderedQueue(): List<QueueItem> {
        return when (orderMode.value) {
            PlaybackQueueStorage.OrderMode.Normal -> database.queueQueries.selectAllById(
                limit = 10000,
                mapper = queueMapper
            )
            PlaybackQueueStorage.OrderMode.Random -> database.queueQueries.selectAllByOrder(
                limit = 10000,
                mapper = queueMapper
            )
        }.executeAsList()
    }

    private suspend fun resetAndPlayFirst() {
        database.queueQueries.shuffleOrder()
        database.queueQueries.updateStatusForAll(QueueItem.State.Pending)
        when (orderMode.value) {
            PlaybackQueueStorage.OrderMode.Normal -> database.queueQueries.selectAllById(
                limit = 1,
                mapper = idMapper
            )
            PlaybackQueueStorage.OrderMode.Random -> database.queueQueries.selectAllByOrder(
                limit = 1,
                mapper = idMapper
            )
        }.executeAsOneOrNull()?.let { id ->
            database.queueQueries.updateStatus(status = QueueItem.State.Playing, id = id)
        }
    }

    private suspend fun resetAndPlayLast() {
        database.queueQueries.shuffleOrder()
        database.queueQueries.updateStatusForAll(QueueItem.State.Finish)
        when (orderMode.value) {
            PlaybackQueueStorage.OrderMode.Normal -> database.queueQueries.selectAllByIdLast(
                limit = 1,
                mapper = idMapper
            )
            PlaybackQueueStorage.OrderMode.Random -> database.queueQueries.selectAllByOrderLast(
                limit = 1,
                mapper = idMapper
            )
        }.executeAsOneOrNull()?.let { id ->
            database.queueQueries.updateStatus(status = QueueItem.State.Playing, id = id)
        }
    }

    private fun getOrder(): PlaybackQueueStorage.OrderMode = preferences.loadInt(ORDER_KEY, -1).toOrderMode()

    private fun saveOrder(orderMode: PlaybackQueueStorage.OrderMode) {
        preferences.saveInt(ORDER_KEY, orderMode.toInt())
    }

    private fun PlaybackQueueStorage.OrderMode.toInt() = when (this) {
        PlaybackQueueStorage.OrderMode.Normal -> 10
        PlaybackQueueStorage.OrderMode.Random -> 20
    }

    private fun Int.toOrderMode() = when (this) {
        20 -> PlaybackQueueStorage.OrderMode.Random
        else -> PlaybackQueueStorage.OrderMode.Normal
    }

    private companion object {
        const val ORDER_KEY = "payback_order"
    }
}
