package by.tigre.music.player.marketing

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
import by.tigre.media.platform.player.view.PlayerView
import by.tigre.media.platform.tools.platform.compose.AppTheme
import by.tigre.music.player.core.presentation.catalog.view.AlbumListView
import by.tigre.music.player.core.presentation.catalog.view.ArtistListView
import by.tigre.music.player.core.presentation.favorites.view.FavoritesView
import by.tigre.music.player.core.presentation.playlist.current.view.CurrentQueueView
import by.tigre.music.player.core.presentation.playlist.library.view.PlaylistsListView
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
    fun artists() = captureScreen("01-artists") {
        val context = composeRule.activity
        ArtistListView(
            component = MarketingScreenshotFixtures.artistListComponent(context, locale),
            albumArtProvider = MarketingScreenshotFixtures.albumArtProvider(context, locale),
        ).Draw(Modifier.fillMaxSize())
    }

    @Test
    fun albums() = captureScreen("02-albums") {
        val context = composeRule.activity
        AlbumListView(
            component = MarketingScreenshotFixtures.albumListComponent(context, locale),
            albumArtProvider = MarketingScreenshotFixtures.albumArtProvider(context, locale),
        ).Draw(Modifier.fillMaxSize())
    }

    @Test
    fun player() = captureScreen("03-player") {
        val context = composeRule.activity
        PlayerView(
            component = MarketingScreenshotFixtures.playerComponent(context, locale),
            config = MarketingScreenshotFixtures.playerViewConfig(locale),
        ).Draw(Modifier.fillMaxSize())
    }

    @Test
    fun queue() = captureScreen("04-queue") {
        val context = composeRule.activity
        CurrentQueueView(
            component = MarketingScreenshotFixtures.currentQueueComponent(locale),
            albumArtProvider = MarketingScreenshotFixtures.albumArtProvider(composeRule.activity, locale),
        ).Draw(Modifier.fillMaxSize())
    }

    @Test
    fun playlists() = captureScreen("05-playlists") {
        PlaylistsListView(
            component = MarketingScreenshotFixtures.playlistsListComponent(locale),
        ).Draw(Modifier.fillMaxSize())
    }

    @Test
    fun favorites() = captureScreen("06-favorites") {
        val context = composeRule.activity
        FavoritesView(
            component = MarketingScreenshotFixtures.favoritesComponent(locale),
            albumArtProvider = MarketingScreenshotFixtures.albumArtProvider(context, locale),
        ).Draw(Modifier.fillMaxSize())
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
        return File("../../docs/marketing/music-player/assets/screenshots/${locale.folderName}").absoluteFile
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
