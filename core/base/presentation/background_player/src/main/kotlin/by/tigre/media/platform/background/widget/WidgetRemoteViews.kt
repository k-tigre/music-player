package by.tigre.media.platform.background.widget

import android.content.Context
import android.widget.RemoteViews
import by.tigre.logger.Log

internal object WidgetRemoteViews {

    fun create(context: Context, layoutName: String): RemoteViews? {
        val appContext = context.applicationContext
        val layoutId = appContext.resources.getIdentifier(layoutName, "layout", appContext.packageName)
        if (layoutId == 0) {
            Log.e("PlaybackWidget") { "Widget layout not found in app package: $layoutName" }
            return null
        }
        return RemoteViews(appContext.packageName, layoutId)
    }
}
