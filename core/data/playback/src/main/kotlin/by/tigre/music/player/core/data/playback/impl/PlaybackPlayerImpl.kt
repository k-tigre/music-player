package by.tigre.music.player.core.data.playback.impl

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import by.tigre.music.player.core.data.entiry.playback.MediaItemWrapper
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.logger.Log
import by.tigre.music.player.tools.coroutines.CoreScope
import by.tigre.music.player.tools.coroutines.extensions.tickerFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

internal class PlaybackPlayerImpl(
    context: Context,
    scope: CoreScope
) : PlaybackPlayer {
    override val state = MutableStateFlow(PlaybackPlayer.State.Idle)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val progress: Flow<PlaybackPlayer.Progress> by lazy {
        state.map {
            it == PlaybackPlayer.State.Paused || it == PlaybackPlayer.State.Playing
        }.distinctUntilChanged()
            .flatMapLatest { withProgress ->
                if (withProgress) {
                    tickerFlow(100.milliseconds)
                        .map {
                            withContext(Dispatchers.Main) {
                                PlaybackPlayer.Progress(player.contentPosition, player.duration)
                            }
                        }
                } else {
                    flowOf(PlaybackPlayer.Progress(0, 0))
                }
            }
            .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)
    }
    private val playerStateListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            handleState(playbackState, player.isPlaying)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            handleState(player.playbackState, isPlaying)
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.i("PlaybackPlayer") { "TEST: onPlayerError - $error" }
            if (player.hasNextMediaItem().not()) {
                state.tryEmit(PlaybackPlayer.State.Ended)
            } else {
                player.stop()
                player.seekToNextMediaItem()
                player.prepare()
                player.play()
            }
        }

        private fun handleState(@Player.State playbackState: Int, isPlaying: Boolean) {
            Log.d("PlaybackPlayer") { "TEST: handleState - $playbackState - $isPlaying" }
            state.tryEmit(
                when (playbackState) {
                    Player.STATE_READY -> if (isPlaying) PlaybackPlayer.State.Playing else PlaybackPlayer.State.Paused
                    Player.STATE_BUFFERING -> PlaybackPlayer.State.Playing
                    Player.STATE_IDLE -> PlaybackPlayer.State.Idle
                    Player.STATE_ENDED -> PlaybackPlayer.State.Ended
                    else -> PlaybackPlayer.State.Idle // impossible case
                }
            )
        }
    }

    override val player: ExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().also { it.addListener(playerStateListener) }
    }

    override suspend fun stop() {
        withContext(Dispatchers.Main) {
            player.playWhenReady = false
            player.stop()
        }
    }

    override suspend fun pause() {
        withContext(Dispatchers.Main) {
            player.playWhenReady = false
        }
    }

    override suspend fun resume() {
        withContext(Dispatchers.Main) {
            player.playWhenReady = true
        }
    }

    override suspend fun seekTo(position: Long) {
        withContext(Dispatchers.Main) {
            player.seekTo(position)
        }
    }

    override suspend fun setMediaItem(item: MediaItemWrapper, position: Long) {
        withContext(Dispatchers.Main) {
            player.setMediaItem(
                MediaItem.Builder()
                    .setUri(item.item.path)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setArtist(item.item.artist)
                            .setAlbumTitle(item.item.album)
                            .setTitle(item.item.name)
                            .build()
                    )
                    .build(),
                position
            )
            player.prepare()
        }
    }
}
