package by.tigre.music.player.core.data.playback

import kotlinx.coroutines.flow.StateFlow

/**
 * In-app playback loudness (0f = silent, 1f = full), independent of the OS master volume.
 * On desktop FFmpeg playback this scales PCM before [javax.sound.sampled.SourceDataLine];
 * on the JDK [javax.sound.sampled.Clip] fallback it uses the line [javax.sound.sampled.FloatControl.Type.MASTER_GAIN]
 * when available.
 */
interface AppPlaybackVolume {
    val playbackVolume: StateFlow<Float>
    fun setPlaybackVolume(volume: Float)
}
