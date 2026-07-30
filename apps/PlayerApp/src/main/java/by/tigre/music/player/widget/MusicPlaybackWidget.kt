package by.tigre.music.player.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import by.tigre.media.platform.background.widget.PlaybackWidgetProvider
import by.tigre.media.platform.entitlements.Feature
import by.tigre.music.player.App
import by.tigre.music.player.MainActivity
import by.tigre.music.player.presentation.background.BackgroundService

class MusicPlaybackWidget : PlaybackWidgetProvider() {
    override fun backgroundServiceClass() = BackgroundService::class.java
    override fun mainActivityClass() = MainActivity::class.java

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (!hasWidgetEntitlement(context)) return
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!hasWidgetEntitlement(context)) return
        super.onReceive(context, intent)
    }

    private fun hasWidgetEntitlement(context: Context): Boolean =
        (context.applicationContext as App).graph.entitlementsRepository.has(Feature.HomeWidget)
}
