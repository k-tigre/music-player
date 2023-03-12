package by.tigre.music.player.core.presentation.catalog.di

import android.content.Context
import by.tigre.music.player.core.data.playback.PlaybackController

interface PlayerDependency {
    val playbackController: PlaybackController
}
