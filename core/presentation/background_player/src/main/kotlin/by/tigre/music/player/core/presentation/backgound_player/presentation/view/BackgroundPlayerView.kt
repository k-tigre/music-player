package by.tigre.music.player.core.presentation.backgound_player.presentation.view

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Rating
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import by.tigre.music.player.core.presentation.backgound_player.presentation.component.BackgroundComponent
import by.tigre.music.player.logger.Log
import by.tigre.music.player.tools.platform.utils.getNotificationManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

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
        val nc = NotificationChannel(NOTIFICATION_CHANEL_ID, "playback", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(nc)
    }

    private val mediaNotificationProvider: MediaNotification.Provider = object : DefaultMediaNotificationProvider(service) {

        override fun getMediaButtons(
            session: MediaSession,
            playerCommands: Player.Commands,
            customLayout: ImmutableList<CommandButton>,
            showPauseButton: Boolean
        ): ImmutableList<CommandButton> {
            Log.d("BackgroundPlayerView") { "getMediaButtons playerCommands=$playerCommands" }
            return super.getMediaButtons(session, playerCommands, customLayout, showPauseButton)
        }

        override fun addNotificationActions(
            mediaSession: MediaSession,
            mediaButtons: ImmutableList<CommandButton>,
            builder: NotificationCompat.Builder,
            actionFactory: MediaNotification.ActionFactory
        ): IntArray {
            Log.d("BackgroundPlayerView") { "addNotificationActions mediaButtons=$mediaButtons" }
            return super.addNotificationActions(mediaSession, mediaButtons, builder, actionFactory)
        }
    }

    fun onCreate() {
        val player = InternalPlayerWrapper(component.getPlayer().player)
        mediaSession = MediaSession.Builder(service, player)
            .setCallback(
                object : MediaSession.Callback {
                    override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                        Log.d("BackgroundPlayerView") { "mediaSession callback onConnect" }
                        return super.onConnect(session, controller)
                    }

                    override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
                        Log.d("BackgroundPlayerView") { "mediaSession callback onPostConnect" }
                        super.onPostConnect(session, controller)
                    }

                    override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
                        Log.d("BackgroundPlayerView") { "mediaSession callback onDisconnected" }
                        super.onDisconnected(session, controller)
                    }

                    override fun onPlayerCommandRequest(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        playerCommand: Int
                    ): Int {
                        Log.d("BackgroundPlayerView") { "mediaSession callback onPlayerCommandRequest" }
                        return super.onPlayerCommandRequest(session, controller, playerCommand)
                    }

                    override fun onSetRating(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        mediaId: String,
                        rating: Rating
                    ): ListenableFuture<SessionResult> {
                        Log.d("BackgroundPlayerView") { "mediaSession callback onSetRating" }
                        return super.onSetRating(session, controller, mediaId, rating)
                    }

                    override fun onSetRating(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        rating: Rating
                    ): ListenableFuture<SessionResult> {
                        Log.d("BackgroundPlayerView") { "mediaSession callback onSetRating" }
                        return super.onSetRating(session, controller, rating)
                    }

                    override fun onCustomCommand(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        customCommand: SessionCommand,
                        args: Bundle
                    ): ListenableFuture<SessionResult> {
                        Log.d("BackgroundPlayerView") { "mediaSession callback onCustomCommand" }
                        return super.onCustomCommand(session, controller, customCommand, args)
                    }

                    override fun onAddMediaItems(
                        mediaSession: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        mediaItems: MutableList<MediaItem>
                    ): ListenableFuture<MutableList<MediaItem>> {
                        Log.d("BackgroundPlayerView") { "mediaSession callback onAddMediaItems" }
                        return super.onAddMediaItems(mediaSession, controller, mediaItems)
                    }

                    override fun onSetMediaItems(
                        mediaSession: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        mediaItems: MutableList<MediaItem>,
                        startIndex: Int,
                        startPositionMs: Long
                    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                        Log.d("BackgroundPlayerView") { "mediaSession callback onSetMediaItems" }
                        return super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
                    }

                    override fun onPlaybackResumption(
                        mediaSession: MediaSession,
                        controller: MediaSession.ControllerInfo
                    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                        Log.d("BackgroundPlayerView") { "mediaSession callback onPlaybackResumption" }
                        return super.onPlaybackResumption(mediaSession, controller)
                    }
                }
            )
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

    private inner class InternalPlayerWrapper(private val player: Player) : Player by player {

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
