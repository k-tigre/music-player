package by.tigre.media.platform.tools.platform.compose.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap

@Composable
actual fun rememberAppIconPainter(): Painter {
    val context = LocalContext.current
    return remember(context.packageName) {
        val drawable = context.packageManager.getApplicationIcon(context.applicationInfo)
        BitmapPainter(drawable.toBitmap().asImageBitmap())
    }
}
