package by.tigre.media.platform.tools.platform.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * When true, an ancestor (e.g. scan progress banner) already clears the status bar.
 * Top bars must not apply status-bar insets again.
 */
val LocalStatusBarInsetHandled = staticCompositionLocalOf { false }

@Composable
fun Modifier.statusBarsPaddingUnlessHandled(): Modifier =
    if (LocalStatusBarInsetHandled.current) this else statusBarsPadding()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appTopBarWindowInsets(): WindowInsets =
    if (LocalStatusBarInsetHandled.current) {
        WindowInsets(0, 0, 0, 0)
    } else {
        TopAppBarDefaults.windowInsets
    }
