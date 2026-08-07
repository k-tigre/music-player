package by.tigre.media.platform.player.di

import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.eq.EqProfileController
import by.tigre.media.platform.playback.eq.EqProfileRepository
import by.tigre.media.platform.player.component.BasePlaybackController
import by.tigre.media.platform.player.component.PlaybackSpeedSource
import by.tigre.media.platform.tools.analytics.common.CommonAnalyticsDependency

interface PlayerDependency : CommonAnalyticsDependency {
    val basePlaybackController: BasePlaybackController
    val playbackEqualizer: PlaybackEqualizer
    val appPlaybackVolume: AppPlaybackVolume?
    val playbackSpeedSource: PlaybackSpeedSource?
        get() = null

    val eqProfileController: EqProfileController
    val eqProfileRepository: EqProfileRepository
    /** Max profiles for current app; Music default 8, Book 16. */
    val eqProfileMaxCount: Int
        get() = 8

    /** Book app: allow Save as book/folder. Music: device only. */
    val eqSupportsContentProfiles: Boolean
        get() = false

    fun hasEqDeviceProfilesAccess(): Boolean = true

    fun requestEqDeviceProfilesPaywall() {}
}
