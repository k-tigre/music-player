package by.tigre.music.player.core.presentation.backgound_player.presentation.view

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import by.tigre.music.player.core.data.playback.AndroidPlaybackPlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import by.tigre.background_player.R
import by.tigre.music.player.core.presentation.backgound_player.presentation.component.BackgroundComponent
import by.tigre.music.player.core.presentation.catalog.component.PlayerItem
import by.tigre.music.player.logger.Log
import by.tigre.music.player.tools.platform.utils.getNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class BackgroundPlayerView(
    private val service: Service,
    private val component: BackgroundComponent,
    private val onIntentProvider: () -> Intent
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val notificationManager = service.getNotificationManager()

    private var mediaSession: MediaSession? = null

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
            component.currentItem.collect { currentPlayerItem.value = it }
        }
        val player = InternalPlayerWrapper(
            (component.getPlayer() as AndroidPlaybackPlayer).player,
            currentPlayerItem
        )
        mediaSession = MediaSession.Builder(service, player)
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
    }

    fun onGetSession(): MediaSession? = mediaSession
    fun mediaNotificationProvider(): MediaNotification.Provider {
        return mediaNotificationProvider
    }

    private inner class InternalPlayerWrapper(
        private val player: Player,
        private val currentPlayerItem: MutableStateFlow<PlayerItem?>
    ) : Player by player {

        // Prefer catalog strings over ExoPlayer-merged ID3 (often wrong encoding for Cyrillic).
        override fun getMediaMetadata(): MediaMetadata {
            val item = currentPlayerItem.value ?: return player.mediaMetadata
            return player.mediaMetadata.buildUpon()
                .setTitle(item.title)
                .setArtist(item.artist ?: item.subtitle)
                .apply {
                    item.album?.let { setAlbumTitle(it) }
                    when (val u = item.coverUri) {
                        is Uri -> setArtworkUri(u)
                        else -> Unit
                    }
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
            return if (command != Player.COMMAND_SEEK_TO_NEXT) player.isCommandAvailable(command) else true
        }

        override fun getAvailableCommands(): Player.Commands {

            val commands = player.availableCommands
            return Player.Commands.Builder()
                .addAll(commands)
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_SEEK_BACK)
                .add(Player.COMMAND_SEEK_FORWARD)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_GET_METADATA)
                .add(Player.COMMAND_STOP)
                .add(Player.COMMAND_INVALID)
                .add(Player.COMMAND_RELEASE)
                .add(Player.COMMAND_GET_TIMELINE)
                .add(Player.COMMAND_GET_TRACKS)
                .build()
        }
    }

    companion object {
        private const val NOTIFICATION_CHANEL_ID = "playback_01"
    }
}
