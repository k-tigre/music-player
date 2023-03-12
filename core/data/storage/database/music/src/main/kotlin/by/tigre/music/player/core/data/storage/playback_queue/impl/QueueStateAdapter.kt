package by.tigre.music.player.core.data.storage.playback_queue.impl

import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import com.squareup.sqldelight.ColumnAdapter

object QueueStateAdapter : ColumnAdapter<PlaybackQueueStorage.QueueItem.State, Long> {
    override fun decode(databaseValue: Long) = when (databaseValue) {
        1L -> PlaybackQueueStorage.QueueItem.State.Playing
        2L -> PlaybackQueueStorage.QueueItem.State.Finish
        else -> PlaybackQueueStorage.QueueItem.State.Pending
    }

    override fun encode(value: PlaybackQueueStorage.QueueItem.State) = when (value) {
        PlaybackQueueStorage.QueueItem.State.Pending -> 0L
        PlaybackQueueStorage.QueueItem.State.Playing -> 1L
        PlaybackQueueStorage.QueueItem.State.Finish -> 2L
    }
}