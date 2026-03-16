package by.tigre.music.player.core.data.playback.di

import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.data.playback.impl.DesktopPlaybackPlayerImpl

class DesktopBasePlaybackModule : BasePlaybackModule {
    override val playbackPlayer: PlaybackPlayer by lazy { DesktopPlaybackPlayerImpl() }
}
