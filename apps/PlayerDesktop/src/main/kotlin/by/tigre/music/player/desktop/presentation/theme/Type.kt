package by.tigre.music.player.desktop.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

// Desktop JVM has no Google Fonts provider. Use system monospace for the compact
// terminal-style UI, falling back to the default M3 scale for everything else.
private val mono = FontFamily.Monospace

internal val DesktopTypography = Typography().let { base ->
    base.copy(
        labelSmall = base.labelSmall.copy(fontFamily = mono),
        labelMedium = base.labelMedium.copy(fontFamily = mono),
        labelLarge = base.labelLarge.copy(fontFamily = mono),
    )
}
