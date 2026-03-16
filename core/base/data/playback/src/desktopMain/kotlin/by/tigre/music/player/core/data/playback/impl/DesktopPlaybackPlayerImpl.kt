package by.tigre.music.player.core.data.playback.impl

import by.tigre.music.player.core.data.playback.MediaItemWrapper
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.logger.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

class DesktopPlaybackPlayerImpl : PlaybackPlayer {

    private val playbackScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override val state = MutableStateFlow(PlaybackPlayer.State.Idle)
    private val _progress = MutableStateFlow(PlaybackPlayer.Progress(0, 0))
    override val progress: Flow<PlaybackPlayer.Progress> = _progress

    // Mirrors ExoPlayer's playWhenReady: true means audio should be playing (not paused/stopped)
    @Volatile private var playWhenReady = false

    @Volatile private var currentLine: SourceDataLine? = null
    @Volatile private var currentUri: String? = null
    @Volatile private var currentPosition: Long = 0
    @Volatile private var currentDurationMs: Long = 0
    @Volatile private var playbackJob: Job? = null

    override suspend fun stop() {
        playWhenReady = false
        cancelPlayback()
        currentUri = null
        state.emit(PlaybackPlayer.State.Idle)
        _progress.emit(PlaybackPlayer.Progress(0, 0))
    }

    override suspend fun pause() {
        playWhenReady = false
        currentLine?.stop()
        state.emit(PlaybackPlayer.State.Paused)
    }

    override suspend fun resume() {
        if (currentUri == null) return
        playWhenReady = true
        if (currentLine != null) {
            // Line exists but was stopped by pause() — just restart it
            currentLine?.start()
            state.emit(PlaybackPlayer.State.Playing)
        } else {
            // No active line: start fresh (first play or after setMediaItem in paused state)
            startPlayback(currentUri!!, currentPosition)
        }
    }

    override suspend fun seekTo(position: Long) {
        val uri = currentUri ?: return
        currentPosition = position
        if (playWhenReady) {
            startPlayback(uri, position)
        } else {
            // Paused: cancel current stream and remember new position
            cancelPlayback()
            _progress.emit(PlaybackPlayer.Progress(position, currentDurationMs))
        }
    }

    override suspend fun setMediaItem(item: MediaItemWrapper, position: Long) {
        currentUri = item.uri
        currentPosition = position
        if (playWhenReady) {
            // Song changed while playing — start new track immediately
            startPlayback(item.uri, position)
        } else {
            // Not playing yet (like ExoPlayer prepare()) — just mark as ready to play
            cancelPlayback()
            state.emit(PlaybackPlayer.State.Paused)
            _progress.emit(PlaybackPlayer.Progress(position, 0))
        }
    }

    private suspend fun cancelPlayback() {
        val line = currentLine
        currentLine = null
        // Flush to unblock any pending write() so the coroutine can be cancelled
        try { line?.flush() } catch (_: Exception) { }
        val job = playbackJob
        playbackJob = null
        job?.cancelAndJoin()
        try { line?.stop() } catch (_: Exception) { }
        try { line?.close() } catch (_: Exception) { }
    }

    private suspend fun startPlayback(uri: String, startPositionMs: Long) {
        cancelPlayback()
        state.emit(PlaybackPlayer.State.Playing)
        playbackJob = playbackScope.launch {
            try {
                playFile(uri, startPositionMs)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("DesktopPlayer") { "Playback error: ${e.message}" }
                state.emit(PlaybackPlayer.State.Ended)
            }
        }
    }

    private suspend fun playFile(uri: String, startPositionMs: Long) = withContext(Dispatchers.IO) {
        val file = File(uri)
        if (!file.exists()) {
            Log.w("DesktopPlayer") { "File not found: $uri" }
            state.emit(PlaybackPlayer.State.Ended)
            return@withContext
        }

        val durationMs = try {
            AudioFileIO.read(file).audioHeader.trackLength * 1000L
        } catch (e: Exception) {
            Log.w("DesktopPlayer") { "Could not read duration: ${e.message}" }
            0L
        }
        currentDurationMs = durationMs

        val rawStream = runInterruptible { AudioSystem.getAudioInputStream(file) }
        val rawFormat = rawStream.format

        val pcmFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            rawFormat.sampleRate,
            16,
            rawFormat.channels,
            rawFormat.channels * 2,
            rawFormat.sampleRate,
            false
        )

        val decodedStream = if (rawFormat.encoding == AudioFormat.Encoding.PCM_SIGNED
            && rawFormat.sampleSizeInBits == 16
        ) {
            rawStream
        } else {
            runInterruptible { AudioSystem.getAudioInputStream(pcmFormat, rawStream) }
        }

        val bytesPerMs = pcmFormat.sampleRate * pcmFormat.frameSize / 1000f

        if (startPositionMs > 0) {
            runInterruptible { decodedStream.skip((startPositionMs * bytesPerMs).toLong()) }
        }

        val info = DataLine.Info(SourceDataLine::class.java, pcmFormat)
        val line = runInterruptible { AudioSystem.getLine(info) } as SourceDataLine
        runInterruptible {
            line.open(pcmFormat, 8192)
            line.start()
        }
        currentLine = line

        var positionMs = startPositionMs
        _progress.emit(PlaybackPlayer.Progress(positionMs, durationMs))

        val buffer = ByteArray(4096)
        try {
            while (isActive) {
                if (!playWhenReady) {
                    // Paused — wait without reading, line was stopped externally by pause()
                    delay(50)
                    continue
                }

                val bytesRead = runInterruptible { decodedStream.read(buffer) }
                if (bytesRead == -1) break

                runInterruptible { line.write(buffer, 0, bytesRead) }
                positionMs += (bytesRead / bytesPerMs).toLong()
                currentPosition = positionMs
                _progress.emit(PlaybackPlayer.Progress(positionMs.coerceAtMost(durationMs), durationMs))
            }

            if (isActive && playWhenReady) {
                runInterruptible { line.drain() }
                state.emit(PlaybackPlayer.State.Ended)
            }
        } finally {
            currentLine = null
            try { line.stop() } catch (_: Exception) { }
            try { line.close() } catch (_: Exception) { }
            try { decodedStream.close() } catch (_: Exception) { }
        }
    }
}
