package by.tigre.media.platform.background.widget

import android.appwidget.AppWidgetManager

internal enum class PlaybackWidgetSize(val layoutName: String) {
    Small("widget_playback_small"),
    Compact("widget_playback_compact"),
    CompactVertical("widget_playback_compact_vertical"),
    Wide("widget_playback_wide"),
    Tall("widget_playback_tall"),
    Medium("widget_playback_medium"),
    Large("widget_playback_large"),
    ;

    companion object {
        fun from(appWidgetManager: AppWidgetManager, appWidgetId: Int): PlaybackWidgetSize {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            if (minWidth == 0 && minHeight == 0) return Small
            val cols = gridCells(minWidth)
            val rows = gridCells(minHeight)
            return when {
                cols <= 1 && rows <= 1 -> Small
                rows <= 1 && cols <= 3 -> Compact
                rows <= 1 && cols >= 4 -> Wide
                cols <= 1 && rows <= 3 -> CompactVertical
                cols <= 1 && rows >= 4 -> Tall
                cols >= 3 && rows >= 2 -> Large
                else -> Medium
            }
        }

        private fun gridCells(sizeDp: Int): Int = ((sizeDp + 30) / 70).coerceAtLeast(1)
    }
}
