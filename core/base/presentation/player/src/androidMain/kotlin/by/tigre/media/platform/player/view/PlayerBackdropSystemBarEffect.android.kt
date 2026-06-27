package by.tigre.media.platform.player.view

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun PlayerBackdropSystemBarEffect(
    statusBarColor: Color,
    lightStatusBarIcons: Boolean,
    enabled: Boolean,
) {
    val view = LocalView.current
    if (view.isInEditMode || !enabled) return

    DisposableEffect(statusBarColor, lightStatusBarIcons) {
        val activity = view.context as? ComponentActivity
        if (activity == null) {
            return@DisposableEffect onDispose {}
        }

        val window = activity.window
        val previousStatusBarColor = window.statusBarColor
        val insetsController = WindowCompat.getInsetsController(window, view)
        val previousLightStatusBars = insetsController.isAppearanceLightStatusBars

        window.statusBarColor = statusBarColor.toArgb()
        insetsController.isAppearanceLightStatusBars = lightStatusBarIcons

        onDispose {
            window.statusBarColor = previousStatusBarColor
            insetsController.isAppearanceLightStatusBars = previousLightStatusBars
        }
    }
}
