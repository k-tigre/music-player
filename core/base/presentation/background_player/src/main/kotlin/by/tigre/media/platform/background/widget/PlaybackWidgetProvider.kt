package by.tigre.media.platform.background.widget

import android.app.Activity
import android.app.Service
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Shared home-screen widget for music and audiobook apps.
 * Each app declares a thin subclass and registers it in the manifest.
 */
abstract class PlaybackWidgetProvider : AppWidgetProvider() {

    abstract fun backgroundServiceClass(): Class<out Service>
    abstract fun mainActivityClass(): Class<out Activity>

    override fun onEnabled(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(ComponentName(appContext, javaClass))
        if (ids.isNotEmpty()) {
            onUpdate(appContext, manager, ids)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        PlaybackWidgetUpdater.applyCacheUpdateNow(
            context = context,
            widgetProviderClass = javaClass,
            backgroundServiceClass = backgroundServiceClass(),
            mainActivityClass = mainActivityClass(),
            appWidgetIds = appWidgetIds,
        )
        PlaybackWidgetUpdater.updateWidgets(
            context = context.applicationContext,
            widgetProviderClass = javaClass,
            backgroundServiceClass = backgroundServiceClass(),
            mainActivityClass = mainActivityClass(),
            appWidgetIds = appWidgetIds,
        )
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        when (intent.action) {
            PlaybackWidgetActions.ACTION_UPDATE -> {
                val manager = AppWidgetManager.getInstance(appContext)
                val ids = manager.getAppWidgetIds(ComponentName(appContext, javaClass))
                if (ids.isNotEmpty()) {
                    PlaybackWidgetUpdater.updateWidgets(
                        context = appContext,
                        widgetProviderClass = javaClass,
                        backgroundServiceClass = backgroundServiceClass(),
                        mainActivityClass = mainActivityClass(),
                        appWidgetIds = ids,
                    )
                }
            }
            PlaybackWidgetActions.ACTION_PLAY_PAUSE,
            PlaybackWidgetActions.ACTION_SKIP_PREV,
            PlaybackWidgetActions.ACTION_SKIP_NEXT -> {
                val pendingResult = goAsync()
                PlaybackWidgetUpdater.handleAction(appContext, intent) {
                    pendingResult.finish()
                }
            }
            else -> super.onReceive(context, intent)
        }
    }
}
