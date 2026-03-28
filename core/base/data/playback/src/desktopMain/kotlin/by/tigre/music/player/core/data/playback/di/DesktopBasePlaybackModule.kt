package by.tigre.music.player.core.data.playback.di

import by.tigre.music.player.core.data.playback.PlaybackEqualizer
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.data.playback.impl.DesktopPlaybackEqualizer
import by.tigre.music.player.core.data.playback.impl.FfmpegDesktopPlaybackPlayer
import by.tigre.music.player.core.data.playback.impl.JdkClipDesktopPlaybackPlayer
import by.tigre.music.player.core.data.playback.prefs.EqualizerPreferences
import by.tigre.music.player.core.data.storage.preferences.Preferences

class DesktopBasePlaybackModule(
    preferences: Preferences,
) : BasePlaybackModule {

    private val equalizerPreferences = EqualizerPreferences(preferences)

    private val ffmpegPlayer = FfmpegDesktopPlaybackPlayer.tryCreate(equalizerPreferences)

    private val jdkPlayer = JdkClipDesktopPlaybackPlayer().takeIf { ffmpegPlayer == null }

    override val playbackPlayer: PlaybackPlayer = ffmpegPlayer ?: jdkPlayer!!

    override val playbackEqualizer: PlaybackEqualizer =
        ffmpegPlayer ?: DesktopPlaybackEqualizer(jdkPlayer!!, equalizerPreferences)
}
