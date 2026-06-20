package by.tigre.music.player.core.data.storage.playback_queue.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import by.tigre.music.player.core.data.storage.music.DatabaseMusic
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage.QueueItem
import by.tigre.media.platform.preferences.Preferences
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

    override val shuffleEnabled = MutableStateFlow(readShuffleEnabled())
    override val repeatMode = MutableStateFlow(readRepeatMode())

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

    override suspend fun setShuffleEnabled(enabled: Boolean) {
        if (enabled && !shuffleEnabled.value) {
            database.queueQueries.transaction {
                database.queueQueries.shuffleOrder()
            }
        } else if (!enabled && shuffleEnabled.value) {
            database.queueQueries.transaction {
                normalizeStatusesForNaturalOrder()
            }
        }
        shuffleEnabled.emit(enabled)
        preferences.saveBoolean(SHUFFLE_KEY, enabled)
        preferences.saveBoolean(SHUFFLE_MIGRATED_KEY, true)
    }

    override suspend fun setRepeatMode(mode: PlaybackQueueStorage.RepeatMode) {
        repeatMode.emit(mode)
        preferences.saveInt(REPEAT_KEY, mode.toInt())
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

    override suspend fun removeSongsByIds(ids: List<Song.Id>) {
        if (ids.isEmpty()) return
        database.queueQueries.transaction {
            ids.forEach { id ->
                database.queueQueries.deleteBySongId(song_id = id.value)
            }
        }
    }

    private fun getPlaybackOrderedQueue(): List<QueueItem> {
        return if (shuffleEnabled.value) {
            database.queueQueries.selectAllByOrder(
                limit = 10000,
                mapper = queueMapper
            )
        } else {
            database.queueQueries.selectAllById(
                limit = 10000,
                mapper = queueMapper
            )
        }.executeAsList()
    }

    private suspend fun normalizeStatusesForNaturalOrder() {
        val queueById = database.queueQueries.selectAllById(
            limit = 10000,
            mapper = queueMapper
        ).executeAsList()
        val currentId = queueById.firstOrNull { it.state == QueueItem.State.Playing }?.id ?: return
        var reachedCurrent = false
        queueById.forEach { item ->
            val newStatus = when {
                item.id == currentId -> {
                    reachedCurrent = true
                    QueueItem.State.Playing
                }
                reachedCurrent -> QueueItem.State.Pending
                else -> QueueItem.State.Finish
            }
            if (item.state != newStatus) {
                database.queueQueries.updateStatus(status = newStatus, id = item.id)
            }
        }
    }

    private suspend fun resetAndPlayFirst() {
        database.queueQueries.transaction {
            if (shuffleEnabled.value) {
                database.queueQueries.shuffleOrder()
            }
            database.queueQueries.updateStatusForAll(QueueItem.State.Pending)
            val firstId = if (shuffleEnabled.value) {
                database.queueQueries.selectAllByOrder(limit = 1, mapper = idMapper)
            } else {
                database.queueQueries.selectAllById(limit = 1, mapper = idMapper)
            }.executeAsOneOrNull()
            firstId?.let { id ->
                database.queueQueries.updateStatus(status = QueueItem.State.Playing, id = id)
            }
        }
    }

    private suspend fun resetAndPlayLast() {
        database.queueQueries.transaction {
            if (shuffleEnabled.value) {
                database.queueQueries.shuffleOrder()
            }
            database.queueQueries.updateStatusForAll(QueueItem.State.Finish)
            val lastId = if (shuffleEnabled.value) {
                database.queueQueries.selectAllByOrderLast(limit = 1, mapper = idMapper)
            } else {
                database.queueQueries.selectAllByIdLast(limit = 1, mapper = idMapper)
            }.executeAsOneOrNull()
            lastId?.let { id ->
                database.queueQueries.updateStatus(status = QueueItem.State.Playing, id = id)
            }
        }
    }

    private fun readShuffleEnabled(): Boolean {
        if (!preferences.loadBoolean(SHUFFLE_MIGRATED_KEY, false)) {
            val legacyShuffle = preferences.loadInt(LEGACY_ORDER_KEY, LEGACY_ORDER_NORMAL) == LEGACY_ORDER_RANDOM
            preferences.saveBoolean(SHUFFLE_KEY, legacyShuffle)
            preferences.saveBoolean(SHUFFLE_MIGRATED_KEY, true)
            return legacyShuffle
        }
        return preferences.loadBoolean(SHUFFLE_KEY, false)
    }

    private fun readRepeatMode(): PlaybackQueueStorage.RepeatMode {
        return preferences.loadInt(REPEAT_KEY, PlaybackQueueStorage.RepeatMode.Off.toInt()).toRepeatMode()
    }

    private fun PlaybackQueueStorage.RepeatMode.toInt() = when (this) {
        PlaybackQueueStorage.RepeatMode.Off -> 0
        PlaybackQueueStorage.RepeatMode.All -> 1
        PlaybackQueueStorage.RepeatMode.One -> 2
    }

    private fun Int.toRepeatMode() = when (this) {
        1 -> PlaybackQueueStorage.RepeatMode.All
        2 -> PlaybackQueueStorage.RepeatMode.One
        else -> PlaybackQueueStorage.RepeatMode.Off
    }

    private companion object {
        const val LEGACY_ORDER_KEY = "payback_order"
        const val LEGACY_ORDER_NORMAL = 10
        const val LEGACY_ORDER_RANDOM = 20
        const val SHUFFLE_KEY = "playback_shuffle"
        const val SHUFFLE_MIGRATED_KEY = "playback_shuffle_migrated"
        const val REPEAT_KEY = "playback_repeat"
    }
}
