package by.tigre.media.platform.background.widget

import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import by.tigre.media.platform.background.R
import by.tigre.logger.Log
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private data class ControllerConnection(
    val controller: MediaController,
    val releaseFuture: ListenableFuture<MediaController>,
)

internal object PlaybackWidgetUpdater {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val widgetUpdateExecutor = Executors.newSingleThreadExecutor()

    fun requestUpdate(context: Context, widgetProviderClass: Class<out AppWidgetProvider>) {
        val appContext = context.applicationContext
        val intent = Intent(PlaybackWidgetActions.ACTION_UPDATE)
            .setComponent(ComponentName(appContext, widgetProviderClass))
            .setPackage(appContext.packageName)
        appContext.sendBroadcast(intent)
    }

    /** Called from [BackgroundPlayerView] — state is already persisted, no MediaController needed. */
    fun pushUpdateFromService(
        context: Context,
        widgetProviderClass: Class<out AppWidgetProvider>,
        backgroundServiceClass: Class<out Service>,
        mainActivityClass: Class<*>,
    ) {
        scheduleWidgetWork {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(ComponentName(appContext, widgetProviderClass))
            if (ids.isNotEmpty()) {
                applyWidgetsFromCache(
                    context = appContext,
                    widgetProviderClass = widgetProviderClass,
                    backgroundServiceClass = backgroundServiceClass,
                    mainActivityClass = mainActivityClass,
                    appWidgetIds = ids,
                )
            }
        }
    }

    /** Synchronous cache-only refresh — safe on the main thread, no MediaController. */
    fun applyCacheUpdateNow(
        context: Context,
        widgetProviderClass: Class<out AppWidgetProvider>,
        backgroundServiceClass: Class<out Service>,
        mainActivityClass: Class<*>,
        appWidgetIds: IntArray,
    ) {
        applyWidgetsFromCache(
            context = context.applicationContext,
            widgetProviderClass = widgetProviderClass,
            backgroundServiceClass = backgroundServiceClass,
            mainActivityClass = mainActivityClass,
            appWidgetIds = appWidgetIds,
        )
    }

    /** Never blocks the caller — safe to invoke from the main thread. */
    fun updateWidgets(
        context: Context,
        widgetProviderClass: Class<out AppWidgetProvider>,
        backgroundServiceClass: Class<out Service>,
        mainActivityClass: Class<*>,
        appWidgetIds: IntArray,
    ) {
        scheduleWidgetWork {
            updateWidgetsInternal(
                context = context.applicationContext,
                widgetProviderClass = widgetProviderClass,
                backgroundServiceClass = backgroundServiceClass,
                mainActivityClass = mainActivityClass,
                appWidgetIds = appWidgetIds,
            )
        }
    }

    fun handleAction(context: Context, intent: Intent, onFinished: () -> Unit = {}) {
        scheduleWidgetWork {
            try {
                handleActionInternal(context.applicationContext, intent)
            } finally {
                onFinished()
            }
        }
    }

    private fun scheduleWidgetWork(block: () -> Unit) {
        widgetUpdateExecutor.execute(block)
    }

    private fun updateWidgetsInternal(
        context: Context,
        widgetProviderClass: Class<out AppWidgetProvider>,
        backgroundServiceClass: Class<out Service>,
        mainActivityClass: Class<*>,
        appWidgetIds: IntArray,
    ) {
        applyWidgetsFromCache(
            context = context,
            widgetProviderClass = widgetProviderClass,
            backgroundServiceClass = backgroundServiceClass,
            mainActivityClass = mainActivityClass,
            appWidgetIds = appWidgetIds,
        )

        // A live MediaController races with MainActivity on startup and can freeze the app.
        // Cache is refreshed by BackgroundPlayerView while the service is running.
        if (PlaybackWidgetStateStore.load(context) != null) return

        val connection = connectController(context, backgroundServiceClass) ?: return
        try {
            val state = runOnMainThread { readState(context, connection.controller) } ?: return
            PlaybackWidgetStateStore.save(context, state.toCached())
            applyWidgetViews(
                context = context,
                widgetProviderClass = widgetProviderClass,
                backgroundServiceClass = backgroundServiceClass,
                mainActivityClass = mainActivityClass,
                appWidgetIds = appWidgetIds,
                state = state,
            )
        } finally {
            runOnMainThread { releaseController(connection) }
        }
    }

    private fun handleActionInternal(context: Context, intent: Intent) {
        val serviceClassName = intent.getStringExtra(PlaybackWidgetActions.EXTRA_BACKGROUND_SERVICE) ?: return
        val widgetProviderClassName = intent.getStringExtra(PlaybackWidgetActions.EXTRA_WIDGET_PROVIDER) ?: return
        val mainActivityClassName = intent.getStringExtra(PlaybackWidgetActions.EXTRA_MAIN_ACTIVITY) ?: return
        val serviceClass = Class.forName(serviceClassName).asSubclass(Service::class.java)
        val widgetProviderClass = Class.forName(widgetProviderClassName).asSubclass(AppWidgetProvider::class.java)
        val mainActivityClass = Class.forName(mainActivityClassName)

        val connection = connectController(context, serviceClass) ?: run {
            refreshWidgetsFromCache(context, widgetProviderClass, serviceClass, mainActivityClass)
            return
        }

        try {
            runOnMainThread {
                val controller = connection.controller
                when (intent.action) {
                    PlaybackWidgetActions.ACTION_PLAY_PAUSE -> {
                        val cached = PlaybackWidgetStateStore.load(context)
                        if (controller.isPlaying) {
                            controller.pause()
                            cached?.let {
                                PlaybackWidgetStateStore.save(context, it.copy(isPlaying = false))
                            }
                        } else {
                            controller.play()
                            cached?.let {
                                PlaybackWidgetStateStore.save(
                                    context,
                                    it.copy(isPlaying = true, hasActiveMedia = true),
                                )
                            }
                        }
                    }
                    PlaybackWidgetActions.ACTION_SKIP_PREV -> controller.seekToPrevious()
                    PlaybackWidgetActions.ACTION_SKIP_NEXT -> controller.seekToNext()
                }
            }
        } finally {
            runOnMainThread { releaseController(connection) }
        }

        refreshWidgetsFromCache(context, widgetProviderClass, serviceClass, mainActivityClass)
    }

    private fun refreshWidgetsFromCache(
        context: Context,
        widgetProviderClass: Class<out AppWidgetProvider>,
        backgroundServiceClass: Class<out Service>,
        mainActivityClass: Class<*>,
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, widgetProviderClass))
        if (ids.isNotEmpty()) {
            applyWidgetsFromCache(context, widgetProviderClass, backgroundServiceClass, mainActivityClass, ids)
        }
    }

    private fun applyWidgetsFromCache(
        context: Context,
        widgetProviderClass: Class<out AppWidgetProvider>,
        backgroundServiceClass: Class<out Service>,
        mainActivityClass: Class<*>,
        appWidgetIds: IntArray,
    ) {
        val state = PlaybackWidgetStateStore.load(context)?.toPlaybackState() ?: idleState(context)
        applyWidgetViews(context, widgetProviderClass, backgroundServiceClass, mainActivityClass, appWidgetIds, state)
    }

    private fun applyWidgetViews(
        context: Context,
        widgetProviderClass: Class<out AppWidgetProvider>,
        backgroundServiceClass: Class<out Service>,
        mainActivityClass: Class<*>,
        appWidgetIds: IntArray,
        state: PlaybackState,
    ) {
        val manager = AppWidgetManager.getInstance(context)
        appWidgetIds.forEach { appWidgetId ->
            val size = PlaybackWidgetSize.from(manager, appWidgetId)
            try {
                val views = buildRemoteViews(
                    context = context,
                    size = size,
                    state = state,
                    mainActivityClass = mainActivityClass,
                    widgetProviderClass = widgetProviderClass,
                    backgroundServiceClass = backgroundServiceClass,
                ) ?: return@forEach
                manager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e("PlaybackWidget") { "Failed to update widget $appWidgetId: $e" }
            }
        }
    }

    /** Must run on a background thread — [ListenableFuture.get] deadlocks on the main thread. */
    private fun connectController(context: Context, serviceClass: Class<out Service>): ControllerConnection? {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "MediaController.connect must not run on the main thread"
        }
        repeat(CONNECT_ATTEMPTS) { attempt ->
            val future = MediaController.Builder(
                context,
                SessionToken(context, ComponentName(context, serviceClass)),
            ).buildAsync()
            try {
                val controller = future.get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                return ControllerConnection(controller, future)
            } catch (_: Exception) {
                MediaController.releaseFuture(future)
                if (attempt < CONNECT_ATTEMPTS - 1) {
                    Thread.sleep(CONNECT_RETRY_DELAY_MS)
                }
            }
        }
        return null
    }

    private fun releaseController(connection: ControllerConnection) {
        MediaController.releaseFuture(connection.releaseFuture)
    }

    private fun <T> runOnMainThread(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return block()
        }
        var result: T? = null
        var error: Throwable? = null
        val latch = CountDownLatch(1)
        mainHandler.post {
            try {
                result = block()
            } catch (t: Throwable) {
                error = t
            } finally {
                latch.countDown()
            }
        }
        if (!latch.await(MAIN_THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw IllegalStateException("Widget MediaController operation timed out")
        }
        error?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private data class PlaybackState(
        val title: String,
        val subtitle: String,
        val artworkUri: android.net.Uri?,
        val isPlaying: Boolean,
        val hasActiveMedia: Boolean,
    ) {
        fun toCached() = CachedPlaybackWidgetState(
            title = title,
            subtitle = subtitle,
            artworkUri = artworkUri,
            isPlaying = isPlaying,
            hasActiveMedia = hasActiveMedia,
        )
    }

    private fun CachedPlaybackWidgetState.toPlaybackState() = PlaybackState(
        title = title,
        subtitle = subtitle,
        artworkUri = artworkUri,
        isPlaying = isPlaying,
        hasActiveMedia = hasActiveMedia,
    )

    private fun idleState(context: Context) = PlaybackState(
        title = context.getString(R.string.widget_idle_title),
        subtitle = context.getString(R.string.widget_idle_subtitle),
        artworkUri = null,
        isPlaying = false,
        hasActiveMedia = false,
    )

    private fun readState(context: Context, controller: MediaController): PlaybackState {
        val metadata = controller.mediaMetadata
        val title = metadata.title?.toString().orEmpty()
            .ifBlank { context.getString(R.string.widget_idle_title) }
        val subtitle = metadata.artist?.toString()
            ?: metadata.albumTitle?.toString()
            ?: context.getString(R.string.widget_idle_subtitle)
        val hasActiveMedia = controller.playbackState != Player.STATE_IDLE &&
            controller.playbackState != Player.STATE_ENDED
        val isPlaying = hasActiveMedia && controller.isPlaying
        return PlaybackState(
            title = title,
            subtitle = subtitle,
            artworkUri = metadata.artworkUri,
            isPlaying = isPlaying,
            hasActiveMedia = hasActiveMedia,
        )
    }

    private fun buildRemoteViews(
        context: Context,
        size: PlaybackWidgetSize,
        state: PlaybackState,
        mainActivityClass: Class<*>,
        widgetProviderClass: Class<out AppWidgetProvider>,
        backgroundServiceClass: Class<out Service>,
    ): RemoteViews? {
        return when (size) {
            PlaybackWidgetSize.Small -> buildSmallViews(
                context, state, mainActivityClass, widgetProviderClass, backgroundServiceClass,
            )
            PlaybackWidgetSize.Compact,
            PlaybackWidgetSize.CompactVertical,
            -> buildCompactStyleViews(
                context = context,
                layoutName = size.layoutName,
                state = state,
                mainActivityClass = mainActivityClass,
                widgetProviderClass = widgetProviderClass,
                backgroundServiceClass = backgroundServiceClass,
                playPauseRequestCode = if (size == PlaybackWidgetSize.Compact) 8 else 9,
                openAppRequestCode = if (size == PlaybackWidgetSize.Compact) 103 else 104,
            )
            PlaybackWidgetSize.Wide -> buildControlsStyleViews(
                context = context,
                layoutName = size.layoutName,
                state = state,
                mainActivityClass = mainActivityClass,
                widgetProviderClass = widgetProviderClass,
                backgroundServiceClass = backgroundServiceClass,
                playPauseRequestCode = 10,
                prevRequestCode = 11,
                nextRequestCode = 12,
                openAppRequestCode = 105,
                hideControlsContainer = false,
            )
            PlaybackWidgetSize.Tall -> buildControlsStyleViews(
                context = context,
                layoutName = size.layoutName,
                state = state,
                mainActivityClass = mainActivityClass,
                widgetProviderClass = widgetProviderClass,
                backgroundServiceClass = backgroundServiceClass,
                playPauseRequestCode = 13,
                prevRequestCode = 14,
                nextRequestCode = 15,
                openAppRequestCode = 106,
                hideControlsContainer = true,
            )
            PlaybackWidgetSize.Medium -> buildControlsStyleViews(
                context = context,
                layoutName = size.layoutName,
                state = state,
                mainActivityClass = mainActivityClass,
                widgetProviderClass = widgetProviderClass,
                backgroundServiceClass = backgroundServiceClass,
                playPauseRequestCode = 2,
                prevRequestCode = 3,
                nextRequestCode = 4,
                openAppRequestCode = 101,
                hideControlsContainer = true,
            )
            PlaybackWidgetSize.Large -> buildLargeViews(
                context, state, mainActivityClass, widgetProviderClass, backgroundServiceClass,
            )
        }
    }

    private fun buildCompactStyleViews(
        context: Context,
        layoutName: String,
        state: PlaybackState,
        mainActivityClass: Class<*>,
        widgetProviderClass: Class<out AppWidgetProvider>,
        backgroundServiceClass: Class<out Service>,
        playPauseRequestCode: Int,
        openAppRequestCode: Int,
    ): RemoteViews? {
        val views = WidgetRemoteViews.create(context, layoutName) ?: return null
        val titleId = WidgetIds.title(context)
        val rootId = WidgetIds.root(context)
        if (titleId == 0 || rootId == 0) return null
        views.setTextViewText(titleId, state.title)
        bindPlayPauseButton(
            views = views,
            context = context,
            isPlaying = state.isPlaying,
            widgetProviderClass = widgetProviderClass,
            backgroundServiceClass = backgroundServiceClass,
            mainActivityClass = mainActivityClass,
            requestCode = playPauseRequestCode,
        )
        views.setOnClickPendingIntent(
            rootId,
            openAppPendingIntent(context, mainActivityClass, openAppRequestCode),
        )
        return views
    }

    private fun buildControlsStyleViews(
        context: Context,
        layoutName: String,
        state: PlaybackState,
        mainActivityClass: Class<*>,
        widgetProviderClass: Class<out AppWidgetProvider>,
        backgroundServiceClass: Class<out Service>,
        playPauseRequestCode: Int,
        prevRequestCode: Int,
        nextRequestCode: Int,
        openAppRequestCode: Int,
        hideControlsContainer: Boolean,
    ): RemoteViews? {
        val views = WidgetRemoteViews.create(context, layoutName) ?: return null
        val titleId = WidgetIds.title(context)
        val rootId = WidgetIds.root(context)
        if (titleId == 0 || rootId == 0) return null
        views.setTextViewText(titleId, state.title)
        bindPlayPauseButton(
            views = views,
            context = context,
            isPlaying = state.isPlaying,
            widgetProviderClass = widgetProviderClass,
            backgroundServiceClass = backgroundServiceClass,
            mainActivityClass = mainActivityClass,
            requestCode = playPauseRequestCode,
        )
        bindSkipControls(
            views = views,
            context = context,
            hasActiveMedia = state.hasActiveMedia,
            widgetProviderClass = widgetProviderClass,
            backgroundServiceClass = backgroundServiceClass,
            mainActivityClass = mainActivityClass,
            prevRequestCode = prevRequestCode,
            nextRequestCode = nextRequestCode,
            hideControlsContainer = hideControlsContainer,
        )
        views.setOnClickPendingIntent(
            rootId,
            openAppPendingIntent(context, mainActivityClass, openAppRequestCode),
        )
        return views
    }

    private fun buildSmallViews(
        context: Context,
        state: PlaybackState,
        mainActivityClass: Class<*>,
        widgetProviderClass: Class<out AppWidgetProvider>,
        backgroundServiceClass: Class<out Service>,
    ): RemoteViews? {
        val views = WidgetRemoteViews.create(context, PlaybackWidgetSize.Small.layoutName) ?: return null
        val playPauseId = WidgetIds.playPause(context)
        val rootId = WidgetIds.root(context)
        if (playPauseId == 0 || rootId == 0) return null
        val playPauseIntent = controlPendingIntent(
            context = context,
            action = PlaybackWidgetActions.ACTION_PLAY_PAUSE,
            widgetProviderClass = widgetProviderClass,
            backgroundServiceClass = backgroundServiceClass,
            mainActivityClass = mainActivityClass,
            requestCode = 1,
        )
        views.setImageViewResource(
            playPauseId,
            if (state.isPlaying) WidgetDrawables.pause else WidgetDrawables.play,
        )
        views.setContentDescription(
            playPauseId,
            context.getString(if (state.isPlaying) R.string.widget_pause else R.string.widget_play),
        )
        views.setOnClickPendingIntent(playPauseId, playPauseIntent)
        views.setOnClickPendingIntent(rootId, playPauseIntent)
        return views
    }

    private fun buildLargeViews(
        context: Context,
        state: PlaybackState,
        mainActivityClass: Class<*>,
        widgetProviderClass: Class<out AppWidgetProvider>,
        backgroundServiceClass: Class<out Service>,
    ): RemoteViews? {
        val views = WidgetRemoteViews.create(context, PlaybackWidgetSize.Large.layoutName) ?: return null
        val titleId = WidgetIds.title(context)
        val subtitleId = WidgetIds.subtitle(context)
        val rootId = WidgetIds.root(context)
        if (titleId == 0 || subtitleId == 0 || rootId == 0) return null
        bindCover(views, context, state.artworkUri)
        views.setTextViewText(titleId, state.title)
        views.setTextViewText(subtitleId, state.subtitle)
        views.setViewVisibility(
            subtitleId,
            if (state.subtitle.isBlank()) View.GONE else View.VISIBLE,
        )
        bindPlayPauseButton(
            views = views,
            context = context,
            isPlaying = state.isPlaying,
            widgetProviderClass = widgetProviderClass,
            backgroundServiceClass = backgroundServiceClass,
            mainActivityClass = mainActivityClass,
            requestCode = 5,
        )
        bindSkipControls(
            views = views,
            context = context,
            hasActiveMedia = state.hasActiveMedia,
            widgetProviderClass = widgetProviderClass,
            backgroundServiceClass = backgroundServiceClass,
            mainActivityClass = mainActivityClass,
            prevRequestCode = 6,
            nextRequestCode = 7,
            hideControlsContainer = true,
        )
        views.setOnClickPendingIntent(
            rootId,
            openAppPendingIntent(context, mainActivityClass, 102),
        )
        return views
    }

    private fun bindCover(views: RemoteViews, context: Context, artworkUri: android.net.Uri?) {
        val coverId = WidgetIds.cover(context)
        if (coverId == 0) return
        val bitmap = WidgetArtworkCache.load(context)
            ?: artworkUri?.let { uri ->
                WidgetArtworkCache.update(context, uri)
                WidgetArtworkCache.load(context)
            }
        if (bitmap != null) {
            views.setImageViewBitmap(coverId, bitmap)
        } else {
            views.setImageViewResource(coverId, WidgetDrawables.coverPlaceholder)
        }
    }

    private fun bindPlayPauseButton(
        views: RemoteViews,
        context: Context,
        isPlaying: Boolean,
        widgetProviderClass: Class<out AppWidgetProvider>,
        backgroundServiceClass: Class<out Service>,
        mainActivityClass: Class<*>,
        requestCode: Int,
    ) {
        val buttonId = WidgetIds.playPause(context)
        if (buttonId == 0) return
        views.setImageViewResource(
            buttonId,
            if (isPlaying) WidgetDrawables.pause else WidgetDrawables.play,
        )
        views.setContentDescription(
            buttonId,
            context.getString(if (isPlaying) R.string.widget_pause else R.string.widget_play),
        )
        views.setOnClickPendingIntent(
            buttonId,
            controlPendingIntent(
                context = context,
                action = PlaybackWidgetActions.ACTION_PLAY_PAUSE,
                widgetProviderClass = widgetProviderClass,
                backgroundServiceClass = backgroundServiceClass,
                mainActivityClass = mainActivityClass,
                requestCode = requestCode,
            ),
        )
    }

    private fun bindSkipControls(
        views: RemoteViews,
        context: Context,
        hasActiveMedia: Boolean,
        widgetProviderClass: Class<out AppWidgetProvider>,
        backgroundServiceClass: Class<out Service>,
        mainActivityClass: Class<*>,
        prevRequestCode: Int,
        nextRequestCode: Int,
        hideControlsContainer: Boolean = false,
    ) {
        val prevId = WidgetIds.skipPrev(context)
        val nextId = WidgetIds.skipNext(context)
        if (prevId == 0 || nextId == 0) return
        val visibility = if (hasActiveMedia) View.VISIBLE else View.INVISIBLE
        views.setViewVisibility(prevId, visibility)
        views.setViewVisibility(nextId, visibility)
        if (hideControlsContainer) {
            val controlsId = WidgetIds.controls(context)
            if (controlsId != 0) {
                views.setViewVisibility(controlsId, visibility)
            }
        }
        views.setImageViewResource(prevId, WidgetDrawables.skipPrevious)
        views.setImageViewResource(nextId, WidgetDrawables.skipNext)
        views.setOnClickPendingIntent(
            prevId,
            controlPendingIntent(
                context = context,
                action = PlaybackWidgetActions.ACTION_SKIP_PREV,
                widgetProviderClass = widgetProviderClass,
                backgroundServiceClass = backgroundServiceClass,
                mainActivityClass = mainActivityClass,
                requestCode = prevRequestCode,
            ),
        )
        views.setOnClickPendingIntent(
            nextId,
            controlPendingIntent(
                context = context,
                action = PlaybackWidgetActions.ACTION_SKIP_NEXT,
                widgetProviderClass = widgetProviderClass,
                backgroundServiceClass = backgroundServiceClass,
                mainActivityClass = mainActivityClass,
                requestCode = nextRequestCode,
            ),
        )
    }

    private fun openAppPendingIntent(context: Context, mainActivityClass: Class<*>, requestCode: Int): PendingIntent {
        val intent = Intent(context, mainActivityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun controlPendingIntent(
        context: Context,
        action: String,
        widgetProviderClass: Class<out AppWidgetProvider>,
        backgroundServiceClass: Class<out Service>,
        mainActivityClass: Class<*>,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, widgetProviderClass).apply {
            this.action = action
            setPackage(context.packageName)
            putExtra(PlaybackWidgetActions.EXTRA_BACKGROUND_SERVICE, backgroundServiceClass.name)
            putExtra(PlaybackWidgetActions.EXTRA_MAIN_ACTIVITY, mainActivityClass.name)
            putExtra(PlaybackWidgetActions.EXTRA_WIDGET_PROVIDER, widgetProviderClass.name)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val CONNECT_ATTEMPTS = 2
    private const val CONNECT_TIMEOUT_SECONDS = 2L
    private const val CONNECT_RETRY_DELAY_MS = 250L
    private const val MAIN_THREAD_TIMEOUT_SECONDS = 10L
}
