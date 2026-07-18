package by.tigre.media.platform.tools.platform.compose.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

@Composable
actual fun rememberAppIconPainter(): Painter {
    val context = LocalContext.current
    return remember(context.packageName) {
        BitmapPainter(loadAppIconBitmap(context).asImageBitmap())
    }
}

private fun loadAppIconBitmap(context: Context): Bitmap {
    loadAppIconDrawable(context)?.toBitmap()?.let { return it }

    val fallbackSize = 128
    return Bitmap.createBitmap(fallbackSize, fallbackSize, Bitmap.Config.ARGB_8888)
}

private fun loadAppIconDrawable(context: Context): Drawable? {
    val appContext = context.applicationContext

    // Prefer the transparent vector foreground over the adaptive icon (colored circle).
    appContext.resources.getIdentifier(
        "ic_launcher_foreground",
        "drawable",
        appContext.packageName,
    ).takeIf { it != 0 }?.let { resId ->
        ContextCompat.getDrawable(appContext, resId)?.let { return it }
    }

    appContext.applicationInfo?.let { appInfo ->
        if (appInfo.icon != 0) {
            ContextCompat.getDrawable(appContext, appInfo.icon)?.let { return it }
        }
        runCatching {
            appContext.packageManager.getApplicationIcon(appInfo)
        }.getOrNull()?.let { return it }
    }

    listOf(
        appContext.resources.getIdentifier("ic_launcher", "mipmap", appContext.packageName),
        appContext.resources.getIdentifier("ic_launcher", "drawable", appContext.packageName),
    ).firstOrNull { it != 0 }?.let { resId ->
        ContextCompat.getDrawable(appContext, resId)?.let { return it }
    }

    return null
}

private fun Drawable.toBitmap(): Bitmap {
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    return toBitmap(width, height)
}
