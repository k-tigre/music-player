package by.tigre.media.platform.background.di

import androidx.media3.common.MediaMetadata
import by.tigre.media.platform.background.car.CarMediaLibrary
import by.tigre.media.platform.player.component.BasePlaybackController

interface PlayerBackgroundDependency {
    val basePlaybackController: BasePlaybackController
    val carMediaLibrary: CarMediaLibrary

    /** [MediaMetadata.MediaType] for Android Auto / Automotive session display. */
    val carSessionMediaType: Int get() = MediaMetadata.MEDIA_TYPE_MUSIC
}
