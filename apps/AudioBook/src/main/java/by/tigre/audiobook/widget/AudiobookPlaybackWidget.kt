package by.tigre.audiobook.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import by.tigre.audiobook.App
import by.tigre.audiobook.MainActivity
import by.tigre.audiobook.core.di.PaywallIntents
import by.tigre.audiobook.presentation.background.BackgroundService
import by.tigre.media.platform.background.widget.PlaybackWidgetProvider
import by.tigre.media.platform.entitlements.Feature

class AudiobookPlaybackWidget : PlaybackWidgetProvider() {
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
        if (hasWidgetEntitlement(context)) {
            super.onReceive(context, intent)
            return
        }
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_ENABLED,
            by.tigre.media.platform.background.widget.PlaybackWidgetActions.ACTION_PLAY_PAUSE,
            by.tigre.media.platform.background.widget.PlaybackWidgetActions.ACTION_SKIP_PREV,
            by.tigre.media.platform.background.widget.PlaybackWidgetActions.ACTION_SKIP_NEXT,
            -> openPaywall(context)
        }
    }

    override fun onEnabled(context: Context) {
        if (!hasWidgetEntitlement(context)) {
            openPaywall(context)
            return
        }
        super.onEnabled(context)
    }

    private fun hasWidgetEntitlement(context: Context): Boolean =
        (context.applicationContext as App)
            .graph
            .entitlementsRepository
            .has(Feature.HomeWidget)

    private fun openPaywall(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(PaywallIntents.EXTRA_FEATURE, Feature.HomeWidget.name)
            putExtra(PaywallIntents.EXTRA_SOURCE, "widget")
        }
        context.startActivity(intent)
    }
}
