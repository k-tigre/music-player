package by.tigre.media.platform.background.widget

import android.content.Context

internal object WidgetIds {
    fun root(context: Context) = id(context, "widget_root")
    fun cover(context: Context) = id(context, "widget_cover")
    fun title(context: Context) = id(context, "widget_title")
    fun subtitle(context: Context) = id(context, "widget_subtitle")
    fun playPause(context: Context) = id(context, "widget_play_pause")
    fun skipPrev(context: Context) = id(context, "widget_skip_prev")
    fun skipNext(context: Context) = id(context, "widget_skip_next")
    fun controls(context: Context) = id(context, "widget_controls")

    private fun id(context: Context, name: String): Int {
        return context.resources.getIdentifier(name, "id", context.packageName)
    }
}
