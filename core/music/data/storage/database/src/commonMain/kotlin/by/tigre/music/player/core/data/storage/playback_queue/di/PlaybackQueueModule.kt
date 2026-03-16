package by.tigre.music.player.core.data.storage.playback_queue.di

import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage

interface PlaybackQueueModule {
    val playbackQueueStorage: PlaybackQueueStorage
}
