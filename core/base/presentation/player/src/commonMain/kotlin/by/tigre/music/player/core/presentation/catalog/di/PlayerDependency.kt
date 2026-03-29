package by.tigre.music.player.core.presentation.catalog.di

import by.tigre.music.player.core.data.playback.PlaybackEqualizer
import by.tigre.music.player.core.presentation.catalog.component.BasePlaybackController

interface PlayerDependency {
    val basePlaybackController: BasePlaybackController
    val playbackEqualizer: PlaybackEqualizer
}
