package by.tigre.music.player.core.data.playback.di

import by.tigre.music.player.core.data.playback.PlaybackEqualizer
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.data.playback.impl.DesktopPlaybackEqualizer
import by.tigre.music.player.core.data.playback.impl.FfmpegDesktopPlaybackPlayer
import by.tigre.music.player.core.data.playback.impl.JdkClipDesktopPlaybackPlayer

class DesktopBasePlaybackModule : BasePlaybackModule {

    private val ffmpegPlayer = FfmpegDesktopPlaybackPlayer.tryCreate()

    private val jdkPlayer = JdkClipDesktopPlaybackPlayer().takeIf { ffmpegPlayer == null }

    override val playbackPlayer: PlaybackPlayer = ffmpegPlayer ?: jdkPlayer!!

    override val playbackEqualizer: PlaybackEqualizer =
        ffmpegPlayer ?: DesktopPlaybackEqualizer(jdkPlayer!!)
}
