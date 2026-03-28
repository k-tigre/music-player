package by.tigre.music.player.desktop.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val DesktopColorScheme = darkColorScheme(
    background = DesktopBg,
    surface = DesktopPanel,
    surfaceVariant = DesktopSurface,
    surfaceContainerLow = DesktopPanel,
    surfaceContainerHighest = DesktopSurface,
    primary = DesktopGreen,
    secondary = DesktopGreen,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = DesktopText,
    onSurface = DesktopText,
    outline = DesktopBorder,
)

@Composable
internal fun DesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DesktopColorScheme,
        typography = DesktopTypography,
        content = content,
    )
}
