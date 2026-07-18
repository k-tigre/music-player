package by.tigre.audiobook.core.di

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaMetadata
import by.tigre.audiobook.BuildConfig
import by.tigre.audiobook.core.data.audiobook.di.AndroidAudiobookCatalogModule
import by.tigre.audiobook.core.data.audiobook.di.AudiobookCatalogModule
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.data.audiobook_playback.di.AudiobookPlaybackModule
import by.tigre.audiobook.core.data.storage.audiobook_catalog.di.AndroidAudiobookCatalogStorageModule
import by.tigre.audiobook.car.AudiobookCarMediaLibrary
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
import by.tigre.media.platform.playback.di.AndroidBasePlaybackModule
import by.tigre.media.platform.playback.di.BasePlaybackModule
import by.tigre.media.platform.preferences.ThemePreferencesStorage
import by.tigre.media.platform.preferences.di.AndroidPreferencesModule
import by.tigre.media.platform.background.di.PlayerBackgroundDependency
import by.tigre.media.platform.player.component.BasePlaybackController
import by.tigre.media.platform.player.component.PlaybackSpeedSource
import by.tigre.media.platform.player.component.PlayerItem
import by.tigre.media.platform.player.component.RepeatMode
import by.tigre.media.platform.player.di.PlayerDependency
import by.tigre.media.platform.tools.analytics.book.BookAnalyticsModule
import by.tigre.media.platform.tools.coroutines.CoroutineModule
import by.tigre.media.platform.tools.platform.compose.ContrastPreference
import by.tigre.media.platform.tools.platform.compose.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
) : PlayerDependency,
    PlayerBackgroundDependency,
    AudiobookCatalogDependency,
    BookAnalyticsModule by analyticsModule,
    AudiobookCatalogModule by audiobookCatalogModule,
    AudiobookPlaybackModule by audiobookPlaybackModule {

    override val playbackEqualizer = basePlaybackModule.playbackEqualizer

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
                controller.currentChapter
            ) { book, chapter ->
                if (book != null && chapter != null) {
                    PlayerItem(
                        title = chapter.title,
                        subtitle = book.title,
                        coverUri = book.coverUri?.let(Uri::parse),
                    )
                } else null
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

    companion object {
        fun create(
            context: Context,
            analyticsModule: BookAnalyticsModule,
        ): ApplicationGraph {
            val preferencesModule = AndroidPreferencesModule(context)
            val coroutineModule = CoroutineModule.Impl()
            val basePlaybackModule =
                AndroidBasePlaybackModule(context, coroutineModule, preferencesModule.preferences)

            val audiobookStorageModule = AndroidAudiobookCatalogStorageModule(context, coroutineModule)
            val audiobookCatalogModule = AndroidAudiobookCatalogModule(context, audiobookStorageModule)
            val audiobookPlaybackModule = AudiobookPlaybackModule.Impl(
                audiobookCatalogStorageModule = audiobookStorageModule,
                audiobookCatalogModule = audiobookCatalogModule,
                basePlaybackModule = basePlaybackModule,
                preferences = preferencesModule.preferences,
                coroutineModule = coroutineModule
            )

            val preferences = preferencesModule.preferences
            val appPlaybackVolume = requireNotNull(basePlaybackModule.appPlaybackVolume) {
                "Audiobook requires in-app playback volume"
            }
            val nightTimerController = createNightTimerController(
                context = context.applicationContext,
                preferences = preferences,
                playbackController = audiobookPlaybackModule.audiobookPlaybackController,
                appPlaybackVolume = appPlaybackVolume,
                scope = coroutineModule.scope,
            )
            val themeSettingsStore = ThemeSettingsStore(ThemePreferencesStorage(preferences))
            val rateAppConfigRepository = RateAppConfigRepository(coroutineModule.scope)
            val catalogScanCoordinator = CatalogScanCoordinatorImpl(
                appContext = context.applicationContext,
                scope = coroutineModule.scope,
                catalogSource = audiobookCatalogModule.audiobookCatalogSource,
            )
            return ApplicationGraph(
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
            )
        }
    }
}
