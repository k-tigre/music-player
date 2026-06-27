package by.tigre.media.platform.tools.platform.compose

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import by.tigre.media.platform.tools.compose.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val manrope = FontFamily(
    Font(
        googleFont = GoogleFont("Manrope"),
        fontProvider = fontProvider,
    ),
)

actual val bodyFontFamily: FontFamily = manrope

actual val displayFontFamily: FontFamily = manrope
