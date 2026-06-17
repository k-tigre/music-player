package by.tigre.media.platform.player.component

import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.player.di.PlayerDependency

interface EqualizerComponent {
    val playbackEqualizer: PlaybackEqualizer
    val appPlaybackVolume: AppPlaybackVolume?
    fun close()

    class Impl(
        dependency: PlayerDependency,
        private val onClose: () -> Unit,
    ) : EqualizerComponent {
        override val playbackEqualizer: PlaybackEqualizer = dependency.playbackEqualizer
        override val appPlaybackVolume: AppPlaybackVolume? = dependency.appPlaybackVolume
        override fun close() = onClose()
    }
}
