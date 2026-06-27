package by.tigre.music.player.core.data.storage.playback_queue.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import by.tigre.music.player.core.data.storage.music.DatabaseMusic
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage.QueueItem
import by.tigre.media.platform.preferences.Preferences
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.entiry.playback.QueueSession
import by.tigre.music.player.core.entiry.playlist.Playlist
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

    init {
        refreshHasQueueItemsPref()
    }

    override fun hasPersistedQueueItems(): Boolean =
        preferences.loadBoolean(HAS_QUEUE_ITEMS_KEY, default = false)

    override fun loadQueueSession(): QueueSession {
        val playlistId = preferences.loadLong(SESSION_PLAYLIST_ID_KEY, NO_PLAYLIST_ID)
        if (playlistId == NO_PLAYLIST_ID) return QueueSession.Plain
        return QueueSession.FromPlaylist(
            playlistId = Playlist.Id(playlistId),
            name = preferences.loadString(SESSION_PLAYLIST_NAME_KEY, "").orEmpty(),
            isDirty = preferences.loadBoolean(SESSION_IS_DIRTY_KEY, default = false),
        )
    }

    override fun saveQueueSession(session: QueueSession) {
        when (session) {
            QueueSession.Plain -> {
                preferences.saveLong(SESSION_PLAYLIST_ID_KEY, NO_PLAYLIST_ID)
                preferences.saveString(SESSION_PLAYLIST_NAME_KEY, null)
                preferences.saveBoolean(SESSION_IS_DIRTY_KEY, false)
            }

            is QueueSession.FromPlaylist -> {
                preferences.saveLong(SESSION_PLAYLIST_ID_KEY, session.playlistId.value)
                preferences.saveString(SESSION_PLAYLIST_NAME_KEY, session.name)
                preferences.saveBoolean(SESSION_IS_DIRTY_KEY, session.isDirty)
            }
        }
    }

    override val shuffleEnabled = MutableStateFlow(readShuffleEnabled())
    override val repeatMode = MutableStateFlow(readRepeatMode())

    override val currentQueue: Flow<List<QueueItem>> = database.queueQueries.selectAllByOrder(
        limit = 10000,
        mapper = queueMapper
    ).asFlow().mapToList(scope.coroutineContext)
        .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    override suspend fun playSongs(items: List<Song.Id>) {
        database.queueQueries.transaction {
            database.queueQueries.deleteAll()
            items.forEachIndexed { index, id ->
                database.queueQueries.insertNewWithSortOrder(
                    song_id = id.value,
                    sort_order = index.toLong(),
                )
            }
            items.firstOrNull()?.let {
                database.queueQueries.updateStatusBySongId(status = QueueItem.State.Playing, song_id = it.value)
            }
        }
        refreshHasQueueItemsPref()
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
        if (items.isEmpty()) return
        database.queueQueries.transaction {
            var sortOrder = database.queueQueries.selectMaxSortOrder()
                .executeAsOne()
                .toInt()
            items.forEach { id ->
                sortOrder += 1
                database.queueQueries.insertNewWithSortOrder(
                    song_id = id.value,
                    sort_order = sortOrder.toLong(),
                )
            }
        }
        refreshHasQueueItemsPref()
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
        refreshHasQueueItemsPref()
    }

    override suspend fun removeQueueEntries(queueEntryIds: List<Long>) {
        if (queueEntryIds.isEmpty()) return
        database.queueQueries.transaction {
            queueEntryIds.forEach { id ->
                database.queueQueries.deleteById(id = id)
            }
        }
        refreshHasQueueItemsPref()
    }

    override suspend fun reorderQueue(queueEntryIdsInOrder: List<Long>) {
        if (queueEntryIdsInOrder.isEmpty()) return
        database.queueQueries.transaction {
            queueEntryIdsInOrder.forEachIndexed { index, id ->
                database.queueQueries.updateQueueSortOrder(
                    sort_order = index.toLong(),
                    id = id,
                )
            }
        }
    }

    override suspend fun queueSongIdsInListOrder(): List<Song.Id> =
        database.queueQueries.selectAllByOrder(
            limit = 10000,
            mapper = { _, _, songId -> Song.Id(songId) },
        ).executeAsList()

    private fun refreshHasQueueItemsPref() {
        val hasItems = database.queueQueries.selectAllById(
            limit = 1,
            mapper = idMapper,
        ).executeAsList().isNotEmpty()
        preferences.saveBoolean(HAS_QUEUE_ITEMS_KEY, hasItems)
    }

    private fun getPlaybackOrderedQueue(): List<QueueItem> =
        database.queueQueries.selectAllByOrder(
            limit = 10000,
            mapper = queueMapper
        ).executeAsList()

    private suspend fun normalizeStatusesForNaturalOrder() {
        val queueByOrder = database.queueQueries.selectAllByOrder(
            limit = 10000,
            mapper = queueMapper
        ).executeAsList()
        val currentId = queueByOrder.firstOrNull { it.state == QueueItem.State.Playing }?.id ?: return
        var reachedCurrent = false
        queueByOrder.forEach { item ->
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
            val firstId = database.queueQueries.selectAllByOrder(limit = 1, mapper = idMapper)
                .executeAsOneOrNull()
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
            val lastId = database.queueQueries.selectAllByOrderLast(limit = 1, mapper = idMapper)
                .executeAsOneOrNull()
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
        const val HAS_QUEUE_ITEMS_KEY = "playback_queue_has_items"
        const val SESSION_PLAYLIST_ID_KEY = "playback_queue_session_playlist_id"
        const val SESSION_PLAYLIST_NAME_KEY = "playback_queue_session_playlist_name"
        const val SESSION_IS_DIRTY_KEY = "playback_queue_session_is_dirty"
        const val NO_PLAYLIST_ID = -1L
    }
}
