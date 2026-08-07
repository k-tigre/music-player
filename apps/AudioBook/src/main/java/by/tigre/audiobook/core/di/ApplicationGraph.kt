package by.tigre.audiobook.core.di

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.media3.common.MediaMetadata
import by.tigre.audiobook.BuildConfig
import by.tigre.audiobook.R as AppR
import by.tigre.audiobook.core.data.audiobook.di.AndroidAudiobookCatalogModule
import by.tigre.audiobook.core.data.audiobook.di.AudiobookCatalogModule
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.data.audiobook_playback.di.AudiobookPlaybackModule
import by.tigre.audiobook.core.data.storage.audiobook_catalog.di.AndroidAudiobookCatalogStorageModule
import by.tigre.audiobook.car.AudiobookCarMediaLibrary
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogDependency
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.CatalogThemeSettings
import by.tigre.audiobook.core.presentation.audiobook_catalog.scan.CatalogScanCoordinator
import by.tigre.audiobook.scan.CatalogScanCoordinatorImpl
import by.tigre.media.platform.background.R
import by.tigre.media.platform.background.car.CarMediaLibrary
import by.tigre.audiobook.nighttimer.NightTimerController
import by.tigre.audiobook.nighttimer.createNightTimerController
import by.tigre.audiobook.platform.AudiobookGuideSettings
import by.tigre.audiobook.platform.AudiobookGuideSettingsImpl
import by.tigre.audiobook.platform.ThemeSettingsStore
import by.tigre.audiobook.settings.RateAppConfigRepository
import by.tigre.logger.Log
import by.tigre.media.platform.billing.AndroidBillingService
import by.tigre.media.platform.entitlements.AppSku
import by.tigre.media.platform.entitlements.EntitlementsRepository
import by.tigre.media.platform.entitlements.Feature
import by.tigre.media.platform.entitlements.PlayEntitlementsRepository
import by.tigre.media.platform.preferences.Preferences
import by.tigre.media.platform.playback.di.AndroidBasePlaybackModule
import by.tigre.media.platform.playback.di.BasePlaybackModule
import by.tigre.media.platform.preferences.ThemePreferencesStorage
import by.tigre.media.platform.preferences.di.AndroidPreferencesModule
import by.tigre.media.platform.background.di.PlayerBackgroundDependency
import by.tigre.media.platform.background.widget.WidgetArtworkCache
import by.tigre.media.platform.player.component.BasePlaybackController
import by.tigre.media.platform.player.component.PlaybackSpeedSource
import by.tigre.media.platform.player.component.PlayerItem
import by.tigre.media.platform.player.component.RepeatMode
import by.tigre.media.platform.player.di.PlayerDependency
import by.tigre.media.platform.tools.analytics.book.BookAnalyticsModule
import by.tigre.media.platform.tools.analytics.common.CommonEvents
import by.tigre.media.platform.tools.coroutines.CoroutineModule
import by.tigre.media.platform.tools.platform.compose.ContrastPreference
import by.tigre.media.platform.tools.platform.compose.ThemeMode
import com.google.android.gms.tasks.Tasks
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
class ApplicationGraph(
    private val appContext: Context,
    private val coroutineScope: kotlinx.coroutines.CoroutineScope,
    private val basePlaybackModule: BasePlaybackModule,
    audiobookCatalogModule: AudiobookCatalogModule,
    audiobookPlaybackModule: AudiobookPlaybackModule,
    analyticsModule: BookAnalyticsModule,
    val nightTimerController: NightTimerController,
    val audiobookGuideSettings: AudiobookGuideSettings,
    val themeSettingsStore: ThemeSettingsStore,
    private val rateAppConfigRepository: RateAppConfigRepository,
    override val catalogScanCoordinator: CatalogScanCoordinator,
    val billingService: AndroidBillingService,
    override val entitlementsRepository: EntitlementsRepository,
    private val preferences: Preferences,
) : PlayerDependency,
    PlayerBackgroundDependency,
    AudiobookCatalogDependency,
    BookAnalyticsModule by analyticsModule,
    AudiobookCatalogModule by audiobookCatalogModule,
    AudiobookPlaybackModule by audiobookPlaybackModule {

    private val _paywallRequests = MutableSharedFlow<PaywallRequest>(extraBufferCapacity = 1)
    val paywallRequests = _paywallRequests.asSharedFlow()

    private val _billingMessages = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val billingMessages = _billingMessages.asSharedFlow()

    private val _tipsCount = MutableStateFlow(preferences.loadInt(TIPS_COUNT_KEY, 0))
    override val tipsCount: StateFlow<Int> = _tipsCount.asStateFlow()

    override fun requestPaywall(
        feature: Feature,
        source: String,
    ) {
        emitPaywallRequest(feature, source, PaywallSection.Plans)
    }

    private fun emitPaywallRequest(
        feature: Feature,
        source: String,
        initialSection: PaywallSection,
    ) {
        if (source != "settings") {
            eventAnalytics.trackEvent(
                CommonEvents.Action.FeatureGateBlocked(
                    feature = feature.name,
                    reason = "requires_purchase",
                    source = source,
                ),
            )
        }
        _paywallRequests.tryEmit(PaywallRequest(feature, source, initialSection))
        Log.i("Entitlements") { "Paywall requested for $feature" }
    }

    override fun requestUpgrade() = requestPaywall(Feature.Equalizer, source = "settings")

    override fun requestTips() {
        emitPaywallRequest(
            feature = Feature.Equalizer,
            source = "settings",
            initialSection = PaywallSection.Tips,
        )
    }

    override fun restorePurchases() {
        coroutineScope.launch {
            runCatching { entitlementsRepository.restore() }
                .onSuccess {
                    eventAnalytics.trackEvent(CommonEvents.Action.PurchaseRestored)
                    _billingMessages.tryEmit(by.tigre.audiobook.R.string.billing_restore_complete)
                }
                .onFailure {
                    Log.w("Entitlements") { "Purchase restore failed: ${it.message}" }
                    _billingMessages.tryEmit(by.tigre.audiobook.R.string.billing_restore_failed)
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

    override val playbackEqualizer = basePlaybackModule.playbackEqualizer

    override val eqProfileController get() = basePlaybackModule.eqProfileController
    override val eqProfileRepository get() = basePlaybackModule.eqProfileRepository
    override val eqProfileMaxCount: Int get() = 16

    override val appPlaybackVolume = basePlaybackModule.appPlaybackVolume

    override val themeSettings: StateFlow<CatalogThemeSettings> =
        themeSettingsStore.state
            .map { settings ->
                CatalogThemeSettings(
                    mode = settings.mode,
                    dynamicColor = settings.dynamicColor,
                    contrast = settings.contrast,
                )
            }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                CatalogThemeSettings(
                    mode = themeSettingsStore.state.value.mode,
                    dynamicColor = themeSettingsStore.state.value.dynamicColor,
                    contrast = themeSettingsStore.state.value.contrast,
                ),
            )

    override fun setThemeMode(mode: ThemeMode) = themeSettingsStore.setThemeMode(mode)

    override fun setDynamicColor(enabled: Boolean) = themeSettingsStore.setDynamicColor(enabled)

    override fun setContrast(contrast: ContrastPreference) = themeSettingsStore.setContrast(contrast)

    override val appVersionName: String = BuildConfig.VERSION_NAME

    override val showRateApp: StateFlow<Boolean> = rateAppConfigRepository.showRateApp

    override fun refreshRateAppFlag() = rateAppConfigRepository.refresh()

    override fun onRateAppClick() {
        val packageName = appContext.packageName
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            appContext.startActivity(marketIntent)
        } catch (e: Exception) {
            Log.e(e) { "Failed to open Play Store market URI, falling back to https" }
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(webIntent)
        }
    }

    override fun copyInstallationIdToClipboard() {
        coroutineScope.launch {
            val id = runCatching {
                withContext(Dispatchers.IO) {
                    Tasks.await(FirebaseInstallations.getInstance().id, 5, TimeUnit.SECONDS)
                }
            }.getOrNull()
            if (id.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        AppR.string.installation_id_copy_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                return@launch
            }
            withContext(Dispatchers.Main) {
                val clipboard =
                    appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("installation_id", id))
                Toast.makeText(
                    appContext,
                    AppR.string.installation_id_copied,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    override val playbackSpeedSource: PlaybackSpeedSource by lazy {
        val controller = audiobookPlaybackController
        object : PlaybackSpeedSource {
            override val playbackSpeed = controller.playbackSpeed
            override fun setPlaybackSpeed(speed: Float) = controller.setPlaybackSpeed(speed)
            override fun resetPlaybackSpeed() = controller.resetPlaybackSpeed()
        }
    }

    override val carSessionMediaType: Int = MediaMetadata.MEDIA_TYPE_AUDIO_BOOK

    override val carMediaLibrary: CarMediaLibrary by lazy {
        AudiobookCarMediaLibrary(
            scope = coroutineScope,
            catalog = audiobookCatalogSource,
            playback = audiobookPlaybackController,
            booksTabTitle = appContext.getString(R.string.car_tab_books),
        )
    }

    override val basePlaybackController: BasePlaybackController by lazy {
        val controller: AudiobookPlaybackController = audiobookPlaybackController
        object : BasePlaybackController {
            override val player = controller.player
            override val currentItem = combine(
                controller.currentBook,
                controller.currentChapter,
            ) { book, chapter -> book to chapter }
                .mapLatest { (book, chapter) ->
                    if (book == null || chapter == null) {
                        null
                    } else {
                        val cover = withContext(Dispatchers.IO) {
                            resolvePlayerCover(book, chapter.fileUri)
                        }
                        PlayerItem(
                            title = chapter.title,
                            subtitle = book.title,
                            coverUri = cover,
                        )
                    }
                }
            override val shuffleEnabled = flowOf(false)
            override val repeatMode = flowOf(RepeatMode.Off)
            override fun playNext() = controller.playNextChapter()
            override fun playPrev() = controller.playPrevChapter()
            override fun playNextRemote() = controller.seekBy(60_000L)
            override fun playPrevRemote() = controller.seekBy(-60_000L)
            override fun pause() = controller.pause()
            override fun resume() = controller.resume()
            override fun stop() = controller.stop()
            override fun toggleShuffle() = Unit
            override fun cycleRepeat() = Unit
            override fun onSeekPositionCommitted(positionMs: Long) =
                controller.persistPlaybackPositionAfterSeek(positionMs)
            override fun seekBy(deltaMs: Long): Boolean {
                controller.seekBy(deltaMs)
                return true
            }
        }
    }

    private suspend fun resolvePlayerCover(book: Book, chapterFileUri: String): File? {
        val storedCover = book.coverUri
        if (storedCover != null) {
            Log.i("CoverArt") { "player resolve book.coverUri=$storedCover" }
            val fromStored = when {
                storedCover.startsWith("content:", ignoreCase = true) ||
                    storedCover.startsWith("file:", ignoreCase = true) ->
                    WidgetArtworkCache.materialize(appContext, Uri.parse(storedCover))
                else -> {
                    val file = File(storedCover)
                    file.takeIf { it.exists() && it.length() > 0L }
                }
            }
            if (fromStored != null) {
                Log.i("CoverArt") { "player cover from stored file=${fromStored.absolutePath}" }
                return fromStored
            }
            Log.w("CoverArt") { "player stored cover failed for $storedCover" }
        } else {
            Log.i("CoverArt") { "player book.coverUri=null chapter=$chapterFileUri" }
        }
        val embedded = WidgetArtworkCache.materializeEmbedded(appContext, Uri.parse(chapterFileUri))
        if (embedded != null) {
            Log.i("CoverArt") { "player cover from embedded file=${embedded.absolutePath}" }
            if (storedCover.isNullOrBlank()) {
                audiobookCatalogSource.updateBookCoverUriIfEmpty(book.id, embedded.absolutePath)
                Log.i("CoverArt") { "persisted embedded cover to book id=${book.id.value}" }
            }
        } else {
            Log.w("CoverArt") { "player no cover (folder+embedded miss) chapter=$chapterFileUri" }
        }
        return embedded
    }

    companion object {
        fun create(
            context: Context,
            analyticsModule: BookAnalyticsModule,
        ): ApplicationGraph {
            val preferencesModule = AndroidPreferencesModule(context)
            val coroutineModule = CoroutineModule.Impl()
            val eqContentKeys = by.tigre.media.platform.playback.eq.MutableEqContentKeyProvider()
            val basePlaybackModule =
                AndroidBasePlaybackModule(
                    context,
                    coroutineModule,
                    preferencesModule.preferences,
                    contentKeyProvider = eqContentKeys,
                )

            val audiobookStorageModule = AndroidAudiobookCatalogStorageModule(context, coroutineModule)
            val audiobookCatalogModule = AndroidAudiobookCatalogModule(context, audiobookStorageModule)
            val audiobookPlaybackModule = AudiobookPlaybackModule.Impl(
                audiobookCatalogStorageModule = audiobookStorageModule,
                audiobookCatalogModule = audiobookCatalogModule,
                basePlaybackModule = basePlaybackModule,
                preferences = preferencesModule.preferences,
                coroutineModule = coroutineModule
            )
            coroutineModule.scope.launch {
                // Touch controller so route/content EQ apply starts, and bind book keys.
                basePlaybackModule.eqProfileController
                audiobookPlaybackModule.audiobookPlaybackController.currentBook.collect { book ->
                    if (book == null) {
                        eqContentKeys.clear()
                    } else {
                        eqContentKeys.setBook(
                            bookId = book.id.value,
                            folderUri = book.folderUri,
                            subPath = book.subPath,
                        )
                    }
                }
            }

            val preferences = preferencesModule.preferences
            val billingService = AndroidBillingService(context.applicationContext)
            val entitlementsRepository = PlayEntitlementsRepository(
                context = context.applicationContext,
                billing = billingService,
                app = AppSku.AudioBook,
            )
            lateinit var requestPaywall: (Feature) -> Unit
            val appPlaybackVolume = requireNotNull(basePlaybackModule.appPlaybackVolume) {
                "Audiobook requires in-app playback volume"
            }
            val nightTimerController = createNightTimerController(
                context = context.applicationContext,
                preferences = preferences,
                playbackController = audiobookPlaybackModule.audiobookPlaybackController,
                appPlaybackVolume = appPlaybackVolume,
                scope = coroutineModule.scope,
                entitlementsRepository = entitlementsRepository,
                onPaywallRequest = { feature -> requestPaywall(feature) },
            )
            val themeSettingsStore = ThemeSettingsStore(ThemePreferencesStorage(preferences))
            val rateAppConfigRepository = RateAppConfigRepository(coroutineModule.scope)
            val catalogScanCoordinator = CatalogScanCoordinatorImpl(
                appContext = context.applicationContext,
                scope = coroutineModule.scope,
                catalogSource = audiobookCatalogModule.audiobookCatalogSource,
            )
            val graph = ApplicationGraph(
                appContext = context.applicationContext,
                coroutineScope = coroutineModule.scope,
                basePlaybackModule = basePlaybackModule,
                audiobookCatalogModule = audiobookCatalogModule,
                audiobookPlaybackModule = audiobookPlaybackModule,
                analyticsModule = analyticsModule,
                nightTimerController = nightTimerController,
                audiobookGuideSettings = AudiobookGuideSettingsImpl(preferences),
                themeSettingsStore = themeSettingsStore,
                rateAppConfigRepository = rateAppConfigRepository,
                catalogScanCoordinator = catalogScanCoordinator,
                billingService = billingService,
                entitlementsRepository = entitlementsRepository,
                preferences = preferences,
            )
            requestPaywall = graph::requestPaywall
            coroutineModule.scope.launch {
                var previousTier = entitlementsRepository.tier.value
                entitlementsRepository.tier
                    .drop(1)
                    .collect { tier ->
                        if (tier != previousTier) {
                            graph.eventAnalytics.trackEvent(
                                CommonEvents.Action.SubscriptionTierChanged(
                                    from = previousTier.name,
                                    to = tier.name,
                                ),
                            )
                            previousTier = tier
                        }
                    }
            }
            coroutineModule.scope.launch {
                billingService.start()
                entitlementsRepository.refresh()
            }
            return graph
        }

        private const val TIPS_COUNT_KEY = "tips_count"
    }
}
