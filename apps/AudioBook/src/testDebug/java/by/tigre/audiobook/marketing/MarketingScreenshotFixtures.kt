package by.tigre.audiobook.marketing

import android.content.Context
import by.tigre.audiobook.R
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.BookListComponent
import by.tigre.audiobook.nighttimer.NightTimerController
import by.tigre.audiobook.nighttimer.NightTimerShakeConfig
import by.tigre.audiobook.nighttimer.NightTimerShakeConfigSource
import by.tigre.audiobook.nighttimer.NightTimerShakeDebugState
import by.tigre.audiobook.nighttimer.NightTimerUiState
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.PlaybackPlayer
import by.tigre.media.platform.player.component.BasePlayerComponent
import by.tigre.media.platform.player.component.PlayerComponent
import by.tigre.media.platform.player.component.PlayerItem
import by.tigre.media.platform.player.component.RepeatMode
import by.tigre.media.platform.player.view.PlayerView
import by.tigre.media.platform.presentation.ScreenContentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

object MarketingScreenshotFixtures {

    private const val CHAPTER_DURATION_MS = 42L * 60 * 1000

    private val warPeaceId = Book.Id(1)
    private val crimeId = Book.Id(2)
    private val margaritaId = Book.Id(3)
    private val nineteenEightyFourId = Book.Id(4)
    private val sherlockId = Book.Id(5)

    private fun str(locale: MarketingScreenshotLocale, key: String, vararg formatArgs: Any): String =
        MarketingScreenshotResources.string(locale, key, *formatArgs)

    private fun coverUri(context: Context, locale: MarketingScreenshotLocale, fileName: String): String =
        MarketingScreenshotResources.coverUri(context, locale, fileName).absolutePath

    private fun warPeaceCoverFile(locale: MarketingScreenshotLocale): String =
        if (locale == MarketingScreenshotLocale.Ru) "war_peace.png" else "war_peace.jpg"

    fun bookListComponent(context: Context, locale: MarketingScreenshotLocale): BookListComponent =
        object : BookListComponent {
            private val books = buildBooks(context, locale)
            private val classicsPath = str(locale, "screenshot_subpath_classics")
            override val screenState: StateFlow<ScreenContentState<BookListComponent.BookListUiState>> =
                MutableStateFlow(
                    ScreenContentState.Content(
                        BookListComponent.BookListUiState(
                            continueListeningBooks = books.filter { it.id == warPeaceId || it.id == sherlockId },
                            continueListeningExpanded = true,
                            rootBooks = books.filter { it.subPath.isEmpty() },
                            grouped = listOf(classicsPath to books.filter { it.subPath.isNotEmpty() }),
                            expanded = setOf(classicsPath),
                            currentBookId = warPeaceId,
                            scrollToBookNonce = 0L,
                        )
                    )
                )

            override fun onBookClicked(book: Book) = Unit
            override fun onManageFolders() = Unit
            override fun retry() = Unit
            override fun toggleGroup(path: String) = Unit
            override fun toggleContinueListening() = Unit
            override fun onScreenShown() = Unit
            override fun focusCurrentBook() = Unit
            override fun dismissContinueListening(book: Book) = Unit
        }

    fun playerComponent(context: Context, locale: MarketingScreenshotLocale): PlayerComponent {
        val warPeace = buildBooks(context, locale).first { it.id == warPeaceId }
        val chapterTitle = str(locale, "screenshot_chapter_title", 12)
        return object : PlayerComponent {
            override val currentItem: StateFlow<PlayerItem?> = MutableStateFlow(
                PlayerItem(
                    title = warPeace.title,
                    subtitle = chapterTitle,
                    coverUri = warPeace.coverUri,
                )
            )
            override val position: StateFlow<BasePlayerComponent.Position> = MutableStateFlow(
                BasePlayerComponent.Position("23:15", "-18:45", "42:00")
            )
            override val fraction: StateFlow<Float> = MutableStateFlow(0.55f)
            override val state: StateFlow<BasePlayerComponent.State> =
                MutableStateFlow(BasePlayerComponent.State.Playing)
            override val shuffleEnabled: StateFlow<Boolean> = MutableStateFlow(false)
            override val repeatMode: StateFlow<RepeatMode> = MutableStateFlow(RepeatMode.Off)
            override val playbackEqualizer: PlaybackEqualizer = StubPlaybackEqualizer
            override val appPlaybackVolume = null
            override val playbackSpeed: StateFlow<Float>? = MutableStateFlow(1.25f)
            override fun pause() = Unit
            override fun play() = Unit
            override fun next() = Unit
            override fun prev() = Unit
            override fun seekBack15Seconds() = Unit
            override fun seekBack1Minute() = Unit
            override fun seekForward15Seconds() = Unit
            override fun seekForward1Minute() = Unit
            override fun toggleShuffle() = Unit
            override fun cycleRepeat() = Unit
            override fun seekTo(fraction: Float) = Unit
            override fun setPlaybackSpeed(speed: Float) = Unit
            override fun resetPlaybackSpeed() = Unit
            override fun showQueue() = Unit
            override fun showEqualizer() = Unit
            override fun showSettings() = Unit
        }
    }

    fun playerViewConfig(context: Context, locale: MarketingScreenshotLocale): PlayerView.Config = PlayerView.Config(
        emptyScreenAction = {},
        emptyScreenTitle = str(locale, "screenshot_player_empty_title"),
        emptyScreenMessage = str(locale, "screenshot_player_empty_message"),
        emptyScreenActionTitle = str(locale, "screenshot_player_empty_action"),
        dynamicBackdropEnabled = true,
        showOrderModeButton = false,
        actionsMode = PlayerView.ActionsMode.SeekButtons,
        seekBack1MinuteLabel = context.getString(R.string.player_seek_back_1_minute),
        seekBack15SecondsLabel = context.getString(R.string.player_seek_back_15_seconds),
        seekForward15SecondsLabel = context.getString(R.string.player_seek_forward_15_seconds),
        seekForward1MinuteLabel = context.getString(R.string.player_seek_forward_1_minute),
        seek15SecondsDurationCaption = context.getString(R.string.player_seek_duration_15_seconds),
        seek1MinuteDurationCaption = context.getString(R.string.player_seek_duration_1_minute),
        equalizerMenuLabel = context.getString(R.string.player_equalizer_menu),
        queueMenuLabel = context.getString(R.string.player_queue_menu),
    )

    fun nightTimerController(): NightTimerController = object : NightTimerController {
        override val uiState: StateFlow<NightTimerUiState> = MutableStateFlow(
            NightTimerUiState(isRunning = false, remainingSeconds = 0),
        )
        override val selectedMinutes: StateFlow<Int> = MutableStateFlow(15)
        override val fadeOutAtEnd: StateFlow<Boolean> = MutableStateFlow(true)
        override val shakeConfig: StateFlow<NightTimerShakeConfig> =
            MutableStateFlow(NightTimerShakeConfig.Default)
        override val shakeConfigSource: StateFlow<NightTimerShakeConfigSource> =
            MutableStateFlow(NightTimerShakeConfigSource.Remote)
        override val shakeConfigFetching: StateFlow<Boolean> = MutableStateFlow(false)
        override val shakeDebugState: StateFlow<NightTimerShakeDebugState> =
            MutableStateFlow(NightTimerShakeDebugState())
        override fun setSelectedMinutes(minutes: Int) = Unit
        override fun setFadeOutAtEnd(enabled: Boolean) = Unit
        override fun startTimer() = Unit
        override fun cancelTimer() = Unit
        override fun updateShakeConfig(config: NightTimerShakeConfig) = Unit
        override fun resetShakeConfigToDefaults() = Unit
        override fun refreshShakeConfig() = Unit
        override fun setShakeTestMode(enabled: Boolean) = Unit
        override fun resetShakeDetection() = Unit
    }

    fun audiobookPlaybackController(locale: MarketingScreenshotLocale): AudiobookPlaybackController {
        val chapters = buildChapters(locale)
        val currentChapter = chapters[11]
        return object : AudiobookPlaybackController {
            override val player: PlaybackPlayer = StubPlaybackPlayer
            override val currentBook: StateFlow<Book?> = MutableStateFlow(null)
            override val currentChapter: StateFlow<Chapter?> = MutableStateFlow(currentChapter)
            override val chapters: StateFlow<List<Chapter>> = MutableStateFlow(chapters)
            override val bookFinishedBannerVisible: StateFlow<Boolean> = MutableStateFlow(false)
            override val playbackSpeed: StateFlow<Float> = MutableStateFlow(1.25f)
            override fun loadBook(book: Book) = Unit
            override fun playBook(book: Book) = Unit
            override fun playBookChapter(bookId: Book.Id, chapterId: Chapter.Id) = Unit
            override fun playNextChapter() = Unit
            override fun playPrevChapter() = Unit
            override fun jumpToChapter(chapterId: Chapter.Id) = Unit
            override fun pause() = Unit
            override fun resume() = Unit
            override fun stop() = Unit
            override fun seekBy(deltaMs: Long) = Unit
            override fun setPlaybackSpeed(speed: Float) = Unit
            override fun resetPlaybackSpeed() = Unit
            override fun persistPlaybackPositionAfterSeek(positionMs: Long) = Unit
            override suspend fun endPlaybackForNightTimer(rewindMs: Long?) = Unit
        }
    }

    private fun buildChapters(locale: MarketingScreenshotLocale): List<Chapter> =
        (1..24).map { index ->
            Chapter(
                id = Chapter.Id(index.toLong()),
                bookId = warPeaceId,
                title = str(locale, "screenshot_chapter_title", index),
                fileUri = "content://screenshot/chapter/$index",
                duration = CHAPTER_DURATION_MS,
                sortOrder = index,
            )
        }

    private fun buildBooks(context: Context, locale: MarketingScreenshotLocale): List<Book> {
        val warPeaceTotal = 24 * CHAPTER_DURATION_MS
        return listOf(
            Book(
                id = warPeaceId,
                title = str(locale, "screenshot_book_war_peace"),
                folderUri = "content://screenshot/folder/1",
                chapterCount = 24,
                subPath = str(locale, "screenshot_subpath_classics"),
                totalDurationMs = warPeaceTotal,
                listenedDurationMs = (warPeaceTotal * 0.42f).toLong(),
                isCompleted = false,
                coverUri = coverUri(context, locale, warPeaceCoverFile(locale)),
            ),
            Book(
                id = crimeId,
                title = str(locale, "screenshot_book_crime_punishment"),
                folderUri = "content://screenshot/folder/1",
                chapterCount = 18,
                subPath = str(locale, "screenshot_subpath_classics"),
                totalDurationMs = 18 * CHAPTER_DURATION_MS,
                listenedDurationMs = 18 * CHAPTER_DURATION_MS,
                isCompleted = true,
                coverUri = coverUri(context, locale, "crime.jpg"),
            ),
            Book(
                id = margaritaId,
                title = str(locale, "screenshot_book_master_margarita"),
                folderUri = "content://screenshot/folder/1",
                chapterCount = 16,
                subPath = "",
                totalDurationMs = 16 * CHAPTER_DURATION_MS,
                listenedDurationMs = (16 * CHAPTER_DURATION_MS * 0.18f).toLong(),
                isCompleted = false,
                coverUri = coverUri(context, locale, "margarita.jpg"),
            ),
            Book(
                id = nineteenEightyFourId,
                title = str(locale, "screenshot_book_1984"),
                folderUri = "content://screenshot/folder/1",
                chapterCount = 12,
                subPath = "",
                totalDurationMs = 12 * CHAPTER_DURATION_MS,
                listenedDurationMs = 0,
                isCompleted = false,
                coverUri = coverUri(context, locale, "1984.jpg"),
            ),
            Book(
                id = sherlockId,
                title = str(locale, "screenshot_book_adventures"),
                folderUri = "content://screenshot/folder/1",
                chapterCount = 14,
                subPath = "",
                totalDurationMs = 14 * CHAPTER_DURATION_MS,
                listenedDurationMs = (14 * CHAPTER_DURATION_MS * 0.67f).toLong(),
                isCompleted = false,
                coverUri = coverUri(context, locale, "sherlock.jpg"),
            ),
        )
    }

    private object StubPlaybackPlayer : PlaybackPlayer {
        override val progress = emptyFlow<PlaybackPlayer.Progress>()
        override val state: StateFlow<PlaybackPlayer.State> = MutableStateFlow(PlaybackPlayer.State.Playing)
        override val playbackSpeed: StateFlow<Float> = MutableStateFlow(1.25f)
        override suspend fun stop() = Unit
        override suspend fun pause() = Unit
        override suspend fun resume() = Unit
        override suspend fun seekTo(position: Long) = Unit
        override suspend fun setMediaItem(item: by.tigre.media.platform.playback.MediaItemWrapper, position: Long) = Unit
        override suspend fun setPlaybackSpeed(speed: Float) = Unit
    }

    private object StubPlaybackEqualizer : PlaybackEqualizer {
        override val isAvailable: StateFlow<Boolean> = MutableStateFlow(true)
        override val presetNames: StateFlow<List<String>> = MutableStateFlow(emptyList())
        override val selectedPresetIndex: StateFlow<Int> = MutableStateFlow(0)
        override val bandCenterHz: StateFlow<List<Float>> = MutableStateFlow(emptyList())
        override val bandGainDb: StateFlow<List<Float>> = MutableStateFlow(emptyList())
        override val builtInPresetBandGainsDb: StateFlow<List<List<Float>>> = MutableStateFlow(emptyList())
        override val customPresetIndex: StateFlow<Int> = MutableStateFlow(-1)
        override val bandGainRangeDb: StateFlow<Pair<Float, Float>> = MutableStateFlow(-12f to 12f)
        override fun selectPreset(index: Int) = Unit
        override fun setBandGainDb(bandIndex: Int, gainDb: Float) = Unit
    }
}
