package by.tigre.music.player.core.presentation.backgound_player.di

import androidx.media3.common.MediaMetadata
import by.tigre.music.player.core.presentation.catalog.component.BasePlaybackController

interface PlayerBackgroundDependency {
    val basePlaybackController: BasePlaybackController

    /** [MediaMetadata.MediaType] for Android Auto / Automotive session display. */
    val carSessionMediaType: Int get() = MediaMetadata.MEDIA_TYPE_MUSIC
}
