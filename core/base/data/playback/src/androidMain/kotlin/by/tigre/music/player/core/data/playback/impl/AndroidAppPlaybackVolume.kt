package by.tigre.music.player.core.data.playback.impl

import androidx.media3.exoplayer.ExoPlayer
import by.tigre.music.player.core.data.playback.AppPlaybackVolume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class AndroidAppPlaybackVolume(
    private val playerProvider: () -> ExoPlayer,
) : AppPlaybackVolume {

    private val _playbackVolume = MutableStateFlow(1f)
    override val playbackVolume: StateFlow<Float> = _playbackVolume.asStateFlow()

    override fun setPlaybackVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        playerProvider().volume = v
        _playbackVolume.value = v
    }
}
