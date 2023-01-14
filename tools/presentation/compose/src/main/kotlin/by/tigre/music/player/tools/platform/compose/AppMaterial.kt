package by.tigre.music.player.tools.platform.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppMaterial {

    @Composable
    fun AppTheme(
        useDarkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit
    ) {
        val colors = if (!useDarkTheme) {
            LightColors
        } else {
            DarkColors
        }

        MaterialTheme(
            colorScheme = colors,
            content = content
        )
    }

    private val LightColors = lightColorScheme(
        primary = Colors.md_theme_light_primary,
        onPrimary = Colors.md_theme_light_onPrimary,
        primaryContainer = Colors.md_theme_light_primaryContainer,
        onPrimaryContainer = Colors.md_theme_light_onPrimaryContainer,
        secondary = Colors.md_theme_light_secondary,
        onSecondary = Colors.md_theme_light_onSecondary,
        secondaryContainer = Colors.md_theme_light_secondaryContainer,
        onSecondaryContainer = Colors.md_theme_light_onSecondaryContainer,
        tertiary = Colors.md_theme_light_tertiary,
        onTertiary = Colors.md_theme_light_onTertiary,
        tertiaryContainer = Colors.md_theme_light_tertiaryContainer,
        onTertiaryContainer = Colors.md_theme_light_onTertiaryContainer,
        error = Colors.md_theme_light_error,
        errorContainer = Colors.md_theme_light_errorContainer,
        onError = Colors.md_theme_light_onError,
        onErrorContainer = Colors.md_theme_light_onErrorContainer,
        background = Colors.md_theme_light_background,
        onBackground = Colors.md_theme_light_onBackground,
        surface = Colors.md_theme_light_surface,
        onSurface = Colors.md_theme_light_onSurface,
        surfaceVariant = Colors.md_theme_light_surfaceVariant,
        onSurfaceVariant = Colors.md_theme_light_onSurfaceVariant,
        outline = Colors.md_theme_light_outline,
        inverseOnSurface = Colors.md_theme_light_inverseOnSurface,
        inverseSurface = Colors.md_theme_light_inverseSurface,
        inversePrimary = Colors.md_theme_light_inversePrimary,
        surfaceTint = Colors.md_theme_light_surfaceTint,
        outlineVariant = Colors.md_theme_light_outlineVariant,
        scrim = Colors.md_theme_light_scrim,
    )


    private val DarkColors = darkColorScheme(
        primary = Colors.md_theme_dark_primary,
        onPrimary = Colors.md_theme_dark_onPrimary,
        primaryContainer = Colors.md_theme_dark_primaryContainer,
        onPrimaryContainer = Colors.md_theme_dark_onPrimaryContainer,
        secondary = Colors.md_theme_dark_secondary,
        onSecondary = Colors.md_theme_dark_onSecondary,
        secondaryContainer = Colors.md_theme_dark_secondaryContainer,
        onSecondaryContainer = Colors.md_theme_dark_onSecondaryContainer,
        tertiary = Colors.md_theme_dark_tertiary,
        onTertiary = Colors.md_theme_dark_onTertiary,
        tertiaryContainer = Colors.md_theme_dark_tertiaryContainer,
        onTertiaryContainer = Colors.md_theme_dark_onTertiaryContainer,
        error = Colors.md_theme_dark_error,
        errorContainer = Colors.md_theme_dark_errorContainer,
        onError = Colors.md_theme_dark_onError,
        onErrorContainer = Colors.md_theme_dark_onErrorContainer,
        background = Colors.md_theme_dark_background,
        onBackground = Colors.md_theme_dark_onBackground,
        surface = Colors.md_theme_dark_surface,
        onSurface = Colors.md_theme_dark_onSurface,
        surfaceVariant = Colors.md_theme_dark_surfaceVariant,
        onSurfaceVariant = Colors.md_theme_dark_onSurfaceVariant,
        outline = Colors.md_theme_dark_outline,
        inverseOnSurface = Colors.md_theme_dark_inverseOnSurface,
        inverseSurface = Colors.md_theme_dark_inverseSurface,
        inversePrimary = Colors.md_theme_dark_inversePrimary,
        surfaceTint = Colors.md_theme_dark_surfaceTint,
        outlineVariant = Colors.md_theme_dark_outlineVariant,
        scrim = Colors.md_theme_dark_scrim,

    )

    private object Colors {
        val md_theme_light_primary = Color(0xFF865300)
        val md_theme_light_onPrimary = Color(0xFFFFFFFF)
        val md_theme_light_primaryContainer = Color(0xFFFFDDB9)
        val md_theme_light_onPrimaryContainer = Color(0xFF2B1700)
        val md_theme_light_secondary = Color(0xFF725C00)
        val md_theme_light_onSecondary = Color(0xFFFFFFFF)
        val md_theme_light_secondaryContainer = Color(0xFFFFE082)
        val md_theme_light_onSecondaryContainer = Color(0xFF231B00)
        val md_theme_light_tertiary = Color(0xFF895100)
        val md_theme_light_onTertiary = Color(0xFFFFFFFF)
        val md_theme_light_tertiaryContainer = Color(0xFFFFDCBC)
        val md_theme_light_onTertiaryContainer = Color(0xFF2C1600)
        val md_theme_light_error = Color(0xFFBA1A1A)
        val md_theme_light_errorContainer = Color(0xFFFFDAD6)
        val md_theme_light_onError = Color(0xFFFFFFFF)
        val md_theme_light_onErrorContainer = Color(0xFF410002)
        val md_theme_light_background = Color(0xFFFFFBFF)
        val md_theme_light_onBackground = Color(0xFF1F1B16)
        val md_theme_light_surface = Color(0xFFFFFBFF)
        val md_theme_light_onSurface = Color(0xFF1F1B16)
        val md_theme_light_surfaceVariant = Color(0xFFF1E0D0)
        val md_theme_light_onSurfaceVariant = Color(0xFF504539)
        val md_theme_light_outline = Color(0xFF827568)
        val md_theme_light_inverseOnSurface = Color(0xFFF9EFE7)
        val md_theme_light_inverseSurface = Color(0xFF352F2A)
        val md_theme_light_inversePrimary = Color(0xFFFFB961)
        val md_theme_light_shadow = Color(0xFF000000)
        val md_theme_light_surfaceTint = Color(0xFF865300)
        val md_theme_light_outlineVariant = Color(0xFFD4C4B5)
        val md_theme_light_scrim = Color(0xFF000000)

        val md_theme_dark_primary = Color(0xFFFFB961)
        val md_theme_dark_onPrimary = Color(0xFF472A00)
        val md_theme_dark_primaryContainer = Color(0xFF663E00)
        val md_theme_dark_onPrimaryContainer = Color(0xFFFFDDB9)
        val md_theme_dark_secondary = Color(0xFFE8C347)
        val md_theme_dark_onSecondary = Color(0xFF3C2F00)
        val md_theme_dark_secondaryContainer = Color(0xFF564500)
        val md_theme_dark_onSecondaryContainer = Color(0xFFFFE082)
        val md_theme_dark_tertiary = Color(0xFFFFB86C)
        val md_theme_dark_onTertiary = Color(0xFF492900)
        val md_theme_dark_tertiaryContainer = Color(0xFF683C00)
        val md_theme_dark_onTertiaryContainer = Color(0xFFFFDCBC)
        val md_theme_dark_error = Color(0xFFFFB4AB)
        val md_theme_dark_errorContainer = Color(0xFF93000A)
        val md_theme_dark_onError = Color(0xFF690005)
        val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
        val md_theme_dark_background = Color(0xFF1F1B16)
        val md_theme_dark_onBackground = Color(0xFFEBE1D9)
        val md_theme_dark_surface = Color(0xFF1F1B16)
        val md_theme_dark_onSurface = Color(0xFFEBE1D9)
        val md_theme_dark_surfaceVariant = Color(0xFF504539)
        val md_theme_dark_onSurfaceVariant = Color(0xFFD4C4B5)
        val md_theme_dark_outline = Color(0xFF9C8E81)
        val md_theme_dark_inverseOnSurface = Color(0xFF1F1B16)
        val md_theme_dark_inverseSurface = Color(0xFFEBE1D9)
        val md_theme_dark_inversePrimary = Color(0xFF865300)
        val md_theme_dark_shadow = Color(0xFF000000)
        val md_theme_dark_surfaceTint = Color(0xFFFFB961)
        val md_theme_dark_outlineVariant = Color(0xFF504539)
        val md_theme_dark_scrim = Color(0xFF000000)


        val seed = Color(0xFFFFA726)
    }
}
