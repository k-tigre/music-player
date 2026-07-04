package by.tigre.audiobook.marketing

import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.AudiobookChapterSelector
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.BookListView
import by.tigre.audiobook.nighttimer.NightTimerSettingsScreen
import by.tigre.audiobook.presentation.player.view.AudiobookPlayerTopBar
import by.tigre.audiobook.theme.AppTheme
import by.tigre.media.platform.player.view.PlayerView
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.jetbrains.compose.resources.PreviewContextConfigurationEffect
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

abstract class MarketingScreenshotTestBase(
    private val locale: MarketingScreenshotLocale,
) {
    @get:Rule(order = 0)
    val registerScreenshotTestActivityRule: TestWatcher = object : TestWatcher() {
        override fun starting(description: Description) {
            val appContext: Context = ApplicationProvider.getApplicationContext()
            Shadows.shadowOf(appContext.packageManager).addActivityIfNotPresent(
                ComponentName(
                    appContext.packageName,
                    ScreenshotTestActivity::class.java.name,
                ),
            )
        }
    }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<ScreenshotTestActivity>()

    @Test
    fun catalog() = captureScreen("01-catalog") {
        val context = composeRule.activity
        BookListView(
            component = MarketingScreenshotFixtures.bookListComponent(context, locale),
        ).Draw(Modifier.fillMaxSize())
    }

    @Test
    fun player() = captureScreen("02-player") {
        val context = composeRule.activity
        val playerComponent = MarketingScreenshotFixtures.playerComponent(context, locale)
        val playbackController = MarketingScreenshotFixtures.audiobookPlaybackController(locale)
        val nightTimerController = MarketingScreenshotFixtures.nightTimerController()
        PlayerView(
            component = playerComponent,
            config = MarketingScreenshotFixtures.playerViewConfig(context, locale),
            chapterTitleContent = { chapterTitle ->
                AudiobookChapterSelector(
                    controller = playbackController,
                    chapterTitle = chapterTitle,
                )
            },
            topBarContent = {
                AudiobookPlayerTopBar(
                    playerComponent = playerComponent,
                    nightTimerController = nightTimerController,
                    onShowCatalog = {},
                    onOpenFolderSettings = {},
                    onOpenNightTimerSettings = {},
                    onOpenPlaybackSpeedSettings = {},
                    onShowEqualizer = {},
                )
            },
        ).Draw(Modifier.fillMaxSize())
    }

    @Test
    fun nightTimer() = captureScreen("03-night-timer") {
        NightTimerSettingsScreen(
            controller = MarketingScreenshotFixtures.nightTimerController(),
            onBack = {},
        )
    }

    @Test
    fun chapters() = captureScreen("04-chapters") {
        MarketingChapterListScreen(
            controller = MarketingScreenshotFixtures.audiobookPlaybackController(locale),
        )
    }

    @Test
    fun widgets() = captureScreen("05-widgets") {
        MarketingWidgetsHomeScreen(
            context = composeRule.activity,
            locale = locale,
        )
    }

    private fun captureScreen(fileName: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
                MarketingScreenshotCoilScope {
                    AppTheme(dynamicColor = false) {
                        Surface(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background)
                                .fillMaxSize()
                        ) {
                            content()
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        val outputFile: File = screenshotsDir().resolve("$fileName.png")
        outputFile.parentFile?.mkdirs()
        composeRule.onRoot().captureRoboImage(outputFile.absolutePath)
    }

    private fun screenshotsDir(): File {
        return File("../../docs/marketing/audiobook/assets/screenshots/${locale.folderName}").absoluteFile
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "ru-rRU-w360dp-h640dp-xxhdpi")
class MarketingScreenshotRuTest : MarketingScreenshotTestBase(MarketingScreenshotLocale.Ru)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "en-rUS-w360dp-h640dp-xxhdpi")
class MarketingScreenshotEnTest : MarketingScreenshotTestBase(MarketingScreenshotLocale.En)
