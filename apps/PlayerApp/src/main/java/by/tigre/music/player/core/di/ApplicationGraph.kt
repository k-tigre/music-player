package by.tigre.music.player.core.di

import android.content.Context
import by.tigre.music.player.core.data.catalog.di.AndroidCatalogModule
import by.tigre.music.player.core.data.catalog.di.CatalogModule
import by.tigre.media.platform.billing.AndroidBillingService
import by.tigre.media.platform.entitlements.AppSku
import by.tigre.media.platform.entitlements.EntitlementsRepository
import by.tigre.media.platform.entitlements.Feature
import by.tigre.media.platform.entitlements.PlayEntitlementsRepository
import by.tigre.media.platform.playback.di.AndroidBasePlaybackModule
import by.tigre.music.player.core.data.playback.di.PlaybackModule
import by.tigre.music.player.core.data.storage.playback_queue.di.AndroidPlaybackQueueModule
import by.tigre.music.player.core.data.storage.playback_queue.di.PlaybackQueueModule
import by.tigre.media.platform.preferences.Preferences
import by.tigre.media.platform.preferences.ThemePreferencesStorage
import by.tigre.media.platform.preferences.di.AndroidPreferencesModule
import by.tigre.music.player.platform.PlayerSettings
import by.tigre.music.player.platform.PlayerSettingsImpl
import by.tigre.music.player.platform.ThemeSettingsStore
import by.tigre.music.player.car.MusicCarMediaLibrary
import by.tigre.media.platform.background.car.CarMediaLibrary
import by.tigre.media.platform.background.di.PlayerBackgroundDependency
import by.tigre.media.platform.player.component.BasePlaybackController
import by.tigre.media.platform.player.component.PlayerItem
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.media.platform.player.di.PlayerDependency
import by.tigre.media.platform.player.component.RepeatMode
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.presentation.root.di.RootDependency
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueDependency
import by.tigre.music.player.core.data.favorites.di.FavoritesModule
import by.tigre.music.player.core.presentation.favorites.di.FavoritesDependency
import by.tigre.music.player.core.data.playlist.di.PlaylistModule
import by.tigre.music.player.core.presentation.playlist.library.di.PlaylistsDependency
import by.tigre.media.platform.tools.analytics.music.MusicAnalyticsModule
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import by.tigre.media.platform.tools.coroutines.CoroutineModule
import by.tigre.music.player.core.data.playback.ActivePlaybackSource
import by.tigre.music.player.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ApplicationGraph(
    val appContext: Context,
    private val coroutineScope: kotlinx.coroutines.CoroutineScope,
    playbackModule: PlaybackModule,
    playbackQueueModule: PlaybackQueueModule,
    catalogModule: CatalogModule,
    analyticsModule: MusicAnalyticsModule,
    private val preferences: Preferences,
    val billingService: AndroidBillingService,
    override val entitlementsRepository: EntitlementsRepository,
) : CatalogDependency,
    PlayerDependency,
    PlayerBackgroundDependency,
    CurrentQueueDependency,
    PlaylistsDependency,
    FavoritesDependency,
    PlaylistModule,
    FavoritesModule,
    RootDependency,
    MusicAnalyticsModule by analyticsModule,
    PlaybackModule by playbackModule,
    PlaybackQueueModule by playbackQueueModule,
    CatalogModule by catalogModule {

    private val _paywallRequests = MutableSharedFlow<PaywallRequest>(extraBufferCapacity = 1)
    val paywallRequests = _paywallRequests.asSharedFlow()

    private val _billingMessages = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val billingMessages = _billingMessages.asSharedFlow()

    private val _tipsCount = MutableStateFlow(preferences.loadInt(TIPS_COUNT_KEY, 0))
    override val tipsCount: StateFlow<Int> = _tipsCount.asStateFlow()

    private val playlistModule = PlaylistModule.Impl(playbackQueueModule, catalogModule)
    private val favoritesModule = FavoritesModule.Impl(playbackQueueModule, catalogModule)

    override val playlistRepository
        get() = playlistModule.playlistRepository

    override val favoritesRepository
        get() = favoritesModule.favoritesRepository

    override val addToPlaylistCoordinator
        get() = playlistModule.addToPlaylistCoordinator

    override val appPlaybackVolume = playbackModule.appPlaybackVolume

    override fun requestPaywall(
        feature: Feature,
        source: String,
        initialSection: PaywallSection,
    ) {
        _paywallRequests.tryEmit(PaywallRequest(feature, source, initialSection))
    }

    override fun requestUpgrade() =
        requestPaywall(Feature.Equalizer, source = "settings", initialSection = PaywallSection.Plans)

    override fun requestTips() = requestPaywall(
        feature = Feature.Equalizer,
        source = "settings",
        initialSection = PaywallSection.Tips,
    )

    override fun restorePurchases() {
        coroutineScope.launch {
            runCatching { entitlementsRepository.restore() }
                .onSuccess {
                    eventAnalytics.trackEvent(MusicEvents.Action.PurchaseRestored)
                    _billingMessages.tryEmit(R.string.billing_restore_complete)
                }
                .onFailure {
                    _billingMessages.tryEmit(R.string.billing_restore_failed)
                }
        }
    }

    fun recordTip() {
        val updatedCount = _tipsCount.value + 1
        preferences.saveInt(TIPS_COUNT_KEY, updatedCount)
        _tipsCount.value = updatedCount
    }

    fun showBillingMessage(messageRes: Int) {
        _billingMessages.tryEmit(messageRes)
    }

    override val playerSettings: PlayerSettings by lazy {
        PlayerSettingsImpl(appContext, preferences)
    }

    override val themeSettingsStore: ThemeSettingsStore by lazy {
        ThemeSettingsStore(ThemePreferencesStorage(preferences))
    }

    override val carMediaLibrary: CarMediaLibrary by lazy {
        MusicCarMediaLibrary(
            context = appContext,
            catalog = catalogSource,
            playback = playbackController,
        )
    }

    override val basePlaybackController: BasePlaybackController by lazy {
        val controller = playbackController
        object : BasePlaybackController {
            override val player = controller.player
            override val currentItem = combine(
                controller.nowPlayingOverlay,
                controller.currentItem,
                controller.interruption,
            ) { overlay, song, interruption ->
                when {
                    overlay != null -> PlayerItem(
                        title = overlay.title,
                        subtitle = overlay.sourceLabel ?: "",
                        isExternal = true,
                        canReturnToQueue = interruption != null,
                    )

                    song != null -> PlayerItem(
                        title = song.name,
                        subtitle = "${song.artist}/${song.album}",
                        artist = song.artist,
                        album = song.album,
                        coverUri = albumArtProvider.albumArtUri(song.albumId),
                    )

                    else -> null
                }
            }
            override val shuffleEnabled = controller.shuffleEnabled
            override val repeatMode = controller.repeatMode.map { it.toUiRepeatMode() }
            override fun playNext() {
                if (controller.activePlaybackSource.value is ActivePlaybackSource.Overlay) {
                    eventAnalytics.trackEvent(
                        MusicEvents.Action.ExternalAudioOverlayEnded(MusicEvents.OverlayEndReason.Next)
                    )
                }
                controller.playNext()
            }
            override fun playPrev() = controller.playPrev()
            override fun pause() = controller.pause()
            override fun resume() = controller.resume()
            override fun stop() = controller.stop()
            override fun toggleShuffle() = controller.toggleShuffle()
            override fun cycleRepeat() = controller.cycleRepeat()
            override fun resumeInterruptedSession() {
                if (controller.interruption.value != null) {
                    eventAnalytics.trackEvent(
                        MusicEvents.Action.ExternalAudioOverlayEnded(MusicEvents.OverlayEndReason.ReturnButton)
                    )
                }
                controller.resumeInterruptedSession()
            }
        }
    }

    private fun PlaybackQueueStorage.RepeatMode.toUiRepeatMode(): RepeatMode = when (this) {
        PlaybackQueueStorage.RepeatMode.Off -> RepeatMode.Off
        PlaybackQueueStorage.RepeatMode.All -> RepeatMode.All
        PlaybackQueueStorage.RepeatMode.One -> RepeatMode.One
    }

    companion object {
        fun create(
            context: Context,
            analyticsModule: MusicAnalyticsModule,
        ): ApplicationGraph {
            val preferencesModule = AndroidPreferencesModule(context)
            val catalogModule = AndroidCatalogModule(context, preferencesModule.preferences)
            val coroutineModule = CoroutineModule.Impl()
            val playbackQueueModule = AndroidPlaybackQueueModule(context, coroutineModule, preferencesModule)
            val basePlaybackModule =
                AndroidBasePlaybackModule(context, coroutineModule, preferencesModule.preferences)
            val playbackModule =
                PlaybackModule.Impl(coroutineModule, playbackQueueModule, catalogModule, basePlaybackModule)

            val billingService = AndroidBillingService(context.applicationContext)
            val entitlementsRepository = PlayEntitlementsRepository(
                context = context.applicationContext,
                billing = billingService,
                app = AppSku.Music,
            )
            val graph = ApplicationGraph(
                appContext = context.applicationContext,
                coroutineScope = coroutineModule.scope,
                playbackModule = playbackModule,
                playbackQueueModule = playbackQueueModule,
                catalogModule = catalogModule,
                analyticsModule = analyticsModule,
                preferences = preferencesModule.preferences,
                billingService = billingService,
                entitlementsRepository = entitlementsRepository,
            )
            coroutineModule.scope.launch {
                billingService.start()
                entitlementsRepository.refresh()
            }
            return graph
        }

        private const val TIPS_COUNT_KEY = "tips_count"
    }
}
