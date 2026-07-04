package by.tigre.media.platform.background.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap

internal object WidgetAppIcon {
    @Volatile
    private var cached: Bitmap? = null

    fun load(context: Context): Bitmap {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: context.packageManager
                .getApplicationIcon(context.applicationInfo)
                .toBitmap()
                .also { cached = it }
        }
    }
}
