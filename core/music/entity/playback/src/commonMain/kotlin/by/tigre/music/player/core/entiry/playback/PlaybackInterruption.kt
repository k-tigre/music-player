package by.tigre.music.player.core.entiry.playback

data class ResumePoint(
    val queueEntryId: Long,
    val positionMs: Long,
)

data class PlaybackInterruption(
    val resumePoint: ResumePoint,
    val wasPlaying: Boolean,
)
