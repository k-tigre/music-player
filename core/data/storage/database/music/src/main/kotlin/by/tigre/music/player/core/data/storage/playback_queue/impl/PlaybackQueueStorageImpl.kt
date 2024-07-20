package by.tigre.music.player.core.data.storage.playback_queue.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import by.tigre.music.player.core.data.storage.music.DatabaseMusic
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage.QueueItem
import by.tigre.music.player.core.data.storage.preferences.Preferences
import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackQueueStorageImpl(
    private val database: DatabaseMusic,
    private val preferences: Preferences,
    scope: CoroutineScope
) : PlaybackQueueStorage {

    override val orderMode = MutableStateFlow(getOrder())

    override val currentQueue: Flow<List<QueueItem>> = orderMode.flatMapLatest { order ->
        val mapper = { id: Long, status: QueueItem.State, songId: Long ->
            QueueItem(id = id, songsId = Song.Id(songId), status)
        }

        when (order) {
            PlaybackQueueStorage.OrderMode.Normal -> database.queueQueries.selectAllById(
                limit = 10000,
                mapper = mapper
            )

            PlaybackQueueStorage.OrderMode.Random -> database.queueQueries.selectAllByOrder(
                limit = 10000,
                mapper = mapper
            )
        }
            .asFlow().mapToList(scope.coroutineContext)
    }.shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

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

    override suspend fun playQueue(queue: List<QueueItem>) {
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

    override suspend fun updateSongStates(finishedId: Long?, playingId: Long, pendingId: Long?) {
        database.queueQueries.transaction {
            finishedId?.let { database.queueQueries.updateStatus(status = QueueItem.State.Finish, id = it) }
            pendingId?.let { database.queueQueries.updateStatus(status = QueueItem.State.Pending, id = it) }
            database.queueQueries.updateStatus(status = QueueItem.State.Playing, id = playingId)
        }
    }

    override suspend fun resetStatusesInQueue() {
        database.queueQueries.transaction {
            database.queueQueries.resetStatus()
        }
    }

    override suspend fun resetAndPlayFirst() {
        database.queueQueries.transaction {
            database.queueQueries.shuffleOrder()
            database.queueQueries.updateStatusForAll(QueueItem.State.Pending)
            val mapper = { id: Long, _: QueueItem.State, _: Long ->
                id
            }
            when (orderMode.value) {
                PlaybackQueueStorage.OrderMode.Normal -> database.queueQueries.selectAllById(
                    limit = 1,
                    mapper = mapper
                )

                PlaybackQueueStorage.OrderMode.Random -> database.queueQueries.selectAllByOrder(
                    limit = 1,
                    mapper = mapper
                )
            }.executeAsOneOrNull()?.let { id ->
                database.queueQueries.updateStatus(status = QueueItem.State.Playing, id = id)
            }
        }
    }

    override suspend fun resetAndPlayLast() {
        database.queueQueries.transaction {
            database.queueQueries.shuffleOrder()
            database.queueQueries.updateStatusForAll(QueueItem.State.Finish)
            val mapper = { id: Long, _: QueueItem.State, _: Long ->
                id
            }
            when (orderMode.value) {
                PlaybackQueueStorage.OrderMode.Normal -> database.queueQueries.selectAllByIdLast(
                    limit = 1,
                    mapper = mapper
                )

                PlaybackQueueStorage.OrderMode.Random -> database.queueQueries.selectAllByOrderLast(
                    limit = 1,
                    mapper = mapper
                )
            }.executeAsOneOrNull()?.let { id ->
                database.queueQueries.updateStatus(status = QueueItem.State.Playing, id = id)
            }
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
