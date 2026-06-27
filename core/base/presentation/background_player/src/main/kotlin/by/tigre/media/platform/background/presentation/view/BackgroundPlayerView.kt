package by.tigre.media.platform.background.presentation.view

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetProvider
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import by.tigre.media.platform.playback.AndroidPlaybackPlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import by.tigre.media.platform.background.car.CarMediaLibrarySessionCallback
import by.tigre.media.platform.background.R
import by.tigre.media.platform.background.presentation.component.BackgroundComponent
import by.tigre.media.platform.background.widget.CachedPlaybackWidgetState
import by.tigre.media.platform.background.widget.PlaybackWidgetStateStore
import by.tigre.media.platform.background.widget.PlaybackWidgetUpdater
import by.tigre.media.platform.background.widget.WidgetArtworkCache
import by.tigre.media.platform.playback.PlaybackPlayer
import by.tigre.media.platform.player.component.PlayerItem
import by.tigre.logger.Log
import by.tigre.media.platform.tools.platform.utils.getNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(UnstableApi::class)
class BackgroundPlayerView(
    private val service: MediaLibraryService,
    private val component: BackgroundComponent,
    private val onIntentProvider: () -> Intent,
    private val widgetProviderClass: Class<out AppWidgetProvider>? = null,
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val notificationManager = service.getNotificationManager()

    private var mediaSession: MediaLibraryService.MediaLibrarySession? = null
    private var wrappedPlayer: InternalPlayerWrapper? = null

    init {
        val nc = NotificationChannel(
            NOTIFICATION_CHANEL_ID,
            service.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(nc)
    }

    private val mediaNotificationProvider: MediaNotification.Provider =
        DefaultMediaNotificationProvider.Builder(service)
            .setChannelId(NOTIFICATION_CHANEL_ID)
            .setChannelName(R.string.notification_channel_name)
            .build()

    fun onCreate() {
        val currentPlayerItem = MutableStateFlow<PlayerItem?>(null)
        scope.launch {
            combine(
                component.currentItem,
                component.getPlayer().state,
            ) { item, playerState -> item to playerState }
                .distinctUntilChanged()
                .debounce(WIDGET_UPDATE_DEBOUNCE_MS)
                .collect { (item, playerState) ->
                    currentPlayerItem.value = item
                    wrappedPlayer?.dispatchMetadataChanged()
                    val artworkUri = coverUri(item?.coverUri)
                    persistWidgetState(item, playerState, artworkUri)
                    withContext(Dispatchers.IO) {
                        WidgetArtworkCache.update(service, artworkUri)
                    }
                    if (widgetProviderClass != null) {
                        requestWidgetUpdate()
                    }
                }
        }
        val player = InternalPlayerWrapper(
            (component.getPlayer() as AndroidPlaybackPlayer).player,
            currentPlayerItem
        ).also { wrappedPlayer = it }
        val callback = CarMediaLibrarySessionCallback(
            scope = scope,
            carMediaLibrary = component.carMediaLibrary,
            carSessionMediaType = component.carSessionMediaType,
        )
        mediaSession = MediaLibraryService.MediaLibrarySession.Builder(service, player, callback)
            .setSessionActivity(
                PendingIntent.getActivity(
                    service,
                    0,
                    onIntentProvider(),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    fun destroy() {
        scope.cancel()
        mediaSession?.release()
        mediaSession = null
        wrappedPlayer = null
    }

    fun onGetSession(): MediaLibraryService.MediaLibrarySession? = mediaSession
    fun mediaNotificationProvider(): MediaNotification.Provider {
        return mediaNotificationProvider
    }

    private fun requestWidgetUpdate() {
        val providerClass = widgetProviderClass ?: return
        val activityComponent = onIntentProvider().component ?: return
        PlaybackWidgetUpdater.pushUpdateFromService(
            context = service,
            widgetProviderClass = providerClass,
            backgroundServiceClass = service.javaClass,
            mainActivityClass = Class.forName(activityComponent.className),
        )
    }

    private fun persistWidgetState(
        item: PlayerItem?,
        playerState: PlaybackPlayer.State,
        artworkUri: Uri?,
    ) {
        val hasActiveMedia = playerState != PlaybackPlayer.State.Idle &&
            playerState != PlaybackPlayer.State.Ended
        val isPlaying = playerState == PlaybackPlayer.State.Playing
        val (title, subtitle) = widgetLabels(item)
        PlaybackWidgetStateStore.save(
            service,
            CachedPlaybackWidgetState(
                title = title,
                subtitle = subtitle,
                artworkUri = artworkUri,
                isPlaying = isPlaying,
                hasActiveMedia = hasActiveMedia,
            ),
        )
    }

    private fun widgetLabels(item: PlayerItem?): Pair<String, String> {
        if (item == null) {
            return service.getString(R.string.widget_idle_title) to
                service.getString(R.string.widget_idle_subtitle)
        }
        return if (component.carSessionMediaType == MediaMetadata.MEDIA_TYPE_AUDIO_BOOK) {
            val bookTitle = item.subtitle.takeIf { it.isNotBlank() } ?: item.title
            val chapterTitle = item.title.takeIf {
                it.isNotBlank() && it != bookTitle
            }.orEmpty()
            bookTitle to chapterTitle
        } else {
            val subtitle = item.artist?.takeIf { it.isNotBlank() } ?: item.subtitle
            item.title to subtitle
        }
    }

    private inner class InternalPlayerWrapper(
        player: Player,
        private val currentPlayerItem: MutableStateFlow<PlayerItem?>
    ) : ForwardingPlayer(player) {

        private val metadataListeners = CopyOnWriteArrayList<Player.Listener>()

        override fun addListener(listener: Player.Listener) {
            metadataListeners.add(listener)
            super.addListener(listener)
        }

        override fun removeListener(listener: Player.Listener) {
            metadataListeners.remove(listener)
            super.removeListener(listener)
        }

        fun dispatchMetadataChanged() {
            val metadata = mediaMetadata
            metadataListeners.forEach { it.onMediaMetadataChanged(metadata) }
        }

        // Prefer catalog strings over ExoPlayer-merged ID3 (often wrong encoding for Cyrillic).
        override fun getMediaMetadata(): MediaMetadata {
            val item = currentPlayerItem.value ?: return super.getMediaMetadata()
            return super.getMediaMetadata().buildUpon()
                .setMediaType(component.carSessionMediaType)
                .setTitle(item.title)
                .setArtist(item.artist ?: item.subtitle)
                .apply {
                    item.album?.let { setAlbumTitle(it) }
                    coverUri(item.coverUri)?.let { setArtworkUri(it) }
                }
                .build()
        }

        override fun seekToNext() {
            component.next()
        }

        override fun seekToPrevious() {
            component.prev()
        }

        override fun play() {
            component.play()
        }

        override fun pause() {
            component.pause()
        }

        override fun seekToPreviousMediaItem() {
            component.prev()
        }

        override fun seekToNextMediaItem() {
            component.next()
        }

        override fun stop() {
            Log.i("BackgroundPlayerView") { "stop" }
            component.pause()
        }

        override fun release() {
            Log.i("BackgroundPlayerView") { "release" }
            component.stop()
        }

        override fun isCommandAvailable(command: Int): Boolean {
            return when (command) {
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                -> true
                else -> super.isCommandAvailable(command)
            }
        }

        override fun getAvailableCommands(): Player.Commands {
            val commands = super.getAvailableCommands()
            return Player.Commands.Builder()
                .addAll(commands)
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_SEEK_BACK)
                .add(Player.COMMAND_SEEK_FORWARD)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_GET_METADATA)
                .add(Player.COMMAND_STOP)
                .add(Player.COMMAND_GET_TIMELINE)
                .add(Player.COMMAND_GET_TRACKS)
                .build()
        }
    }

    companion object {
        private const val NOTIFICATION_CHANEL_ID = "playback_01"
        private const val WIDGET_UPDATE_DEBOUNCE_MS = 400L

        internal fun coverUri(cover: Any?): Uri? = when (cover) {
            is Uri -> cover
            is String -> cover.takeIf { it.isNotBlank() }?.let(Uri::parse)
            else -> null
        }
    }
}
