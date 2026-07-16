package by.tigre.media.platform.playback.di

import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.AudioSpectrumSource
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.PlaybackPlayer
import by.tigre.media.platform.playback.impl.DesktopPlaybackEqualizer
import by.tigre.media.platform.playback.impl.FfmpegDesktopPlaybackPlayer
import by.tigre.media.platform.playback.impl.JdkClipDesktopPlaybackPlayer
import by.tigre.media.platform.playback.impl.NoOpAudioSpectrumSource
import by.tigre.media.platform.playback.prefs.EqualizerPreferences
import by.tigre.media.platform.playback.prefs.PlaybackVolumePreferences
import by.tigre.media.platform.playback.prefs.VisualizerPreferences
import by.tigre.media.platform.preferences.Preferences

class DesktopBasePlaybackModule(
    preferences: Preferences,
) : BasePlaybackModule {

    private val equalizerPreferences = EqualizerPreferences(preferences)
    private val volumePreferences = PlaybackVolumePreferences(preferences)
    private val visualizerPrefs = VisualizerPreferences(preferences)

    private val ffmpegPlayer = FfmpegDesktopPlaybackPlayer.tryCreate(equalizerPreferences, volumePreferences)

    private val jdkPlayer = JdkClipDesktopPlaybackPlayer(volumePreferences).takeIf { ffmpegPlayer == null }

    override val playbackPlayer: PlaybackPlayer = ffmpegPlayer ?: jdkPlayer!!

    override val playbackEqualizer: PlaybackEqualizer =
        ffmpegPlayer ?: DesktopPlaybackEqualizer(jdkPlayer!!, equalizerPreferences)

    override val audioSpectrumSource: AudioSpectrumSource = NoOpAudioSpectrumSource()

    override val visualizerPreferences: VisualizerPreferences get() = visualizerPrefs

    override val appPlaybackVolume: AppPlaybackVolume =
        (ffmpegPlayer ?: jdkPlayer!!) as AppPlaybackVolume
}
