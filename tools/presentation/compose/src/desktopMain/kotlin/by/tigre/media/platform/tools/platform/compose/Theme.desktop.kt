package by.tigre.media.platform.tools.platform.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun platformColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme? = null

@Composable
actual fun PlatformThemeEffect(colorScheme: ColorScheme, darkTheme: Boolean) {
    // No-op on Desktop
}
