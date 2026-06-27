package by.tigre.music.player.core.entiry.playback

import by.tigre.music.player.core.entiry.catalog.Song

data class NowPlayingQueueEntry(
    val id: Long,
    val song: Song,
    val isPlaying: Boolean,
    val isInterruptedActive: Boolean,
    val interruptedPositionMs: Long? = null,
)

data class OverlayQueueEntry(
    val item: PlayableItem.ExternalAudio,
    val isPlaying: Boolean,
)

data class NowPlayingScreenModel(
    val session: QueueSession,
    val overlay: OverlayQueueEntry?,
    val queue: List<NowPlayingQueueEntry>,
)
