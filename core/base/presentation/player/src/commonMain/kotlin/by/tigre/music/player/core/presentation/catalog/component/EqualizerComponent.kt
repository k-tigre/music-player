package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.data.playback.AppPlaybackVolume
import by.tigre.music.player.core.data.playback.PlaybackEqualizer
import by.tigre.music.player.core.presentation.catalog.di.PlayerDependency

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
