package by.tigre.music.player.presentation.root.di

import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.platform.PlayerSettings
import by.tigre.music.player.platform.ThemeSettingsStore
import by.tigre.music.player.core.data.playlist.PlaylistRepository
import by.tigre.music.player.core.di.PaywallSection
import by.tigre.media.platform.entitlements.EntitlementsRepository
import by.tigre.media.platform.tools.analytics.music.MusicAnalyticsDependency
import kotlinx.coroutines.flow.StateFlow

interface RootDependency : MusicAnalyticsDependency {
    val playbackQueueStorage: PlaybackQueueStorage
    val playlistRepository: PlaylistRepository
    val playerSettings: PlayerSettings
    val themeSettingsStore: ThemeSettingsStore
    val entitlementsRepository: EntitlementsRepository
    val tipsCount: StateFlow<Int>
    fun requestPaywall(
        feature: by.tigre.media.platform.entitlements.Feature,
        source: String = feature.name,
        initialSection: PaywallSection = PaywallSection.Plans,
    )
    fun requestUpgrade()
    fun restorePurchases()
    fun requestTips()
}
