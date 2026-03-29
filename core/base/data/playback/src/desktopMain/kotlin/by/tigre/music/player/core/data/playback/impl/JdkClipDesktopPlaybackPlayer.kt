package by.tigre.music.player.core.data.playback.impl

import by.tigre.music.player.core.data.playback.MediaItemWrapper
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.data.playback.impl.dsp.DesktopEqualizerPresets
import by.tigre.music.player.core.data.playback.impl.dsp.EqualizingPcmAudioInputStream
import by.tigre.music.player.logger.Log
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
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
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.UnsupportedAudioFileException

internal class JdkClipDesktopPlaybackPlayer : PlaybackPlayer {

    private val playbackScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override val state = MutableStateFlow(PlaybackPlayer.State.Idle)
    private val _progress = MutableStateFlow(PlaybackPlayer.Progress(0, 0))
    override val progress: Flow<PlaybackPlayer.Progress> = _progress

    @Volatile
    private var playWhenReady = false

    @Volatile
    private var currentClip: Clip? = null
    @Volatile
    private var currentUri: String? = null
    @Volatile
    private var currentPosition: Long = 0
    @Volatile
    private var currentDurationMs: Long = 0
    @Volatile
    private var progressJob: Job? = null

    @Volatile
    private var equalizerGains: FloatArray = DesktopEqualizerPresets.gainsForPreset(0)

    /** While mutating clip (seek stop/set/start), [!Clip.isRunning] must not be treated as track finished. */
    @Volatile
    private var suppressTrackEndedUntilMs: Long = 0L

    /** After a user seek, do not emit [PlaybackPlayer.State.Ended] (slider at EOF is not "track finished"). */
    @Volatile
    private var suppressEndedAfterUserSeekUntilMs: Long = 0L

    override suspend fun stop() {
        playWhenReady = false
        suppressEndedAfterUserSeekUntilMs = 0L
        stopProgressJob()
        closeClip()
        currentUri = null
        state.emit(PlaybackPlayer.State.Idle)
        _progress.emit(PlaybackPlayer.Progress(0, 0))
    }

    override suspend fun pause() {
        playWhenReady = false
        runInterruptible(Dispatchers.IO) {
            try {
                currentClip?.stop()
            } catch (_: Exception) {
            }
        }
        state.emit(PlaybackPlayer.State.Paused)
    }

    override suspend fun resume() {
        if (currentUri == null) return
        playWhenReady = true
        val clip = currentClip ?: run {
            openClipForUri(currentUri!!, currentPosition)
            currentClip
        } ?: return
        val rewindFromEof =
            state.value != PlaybackPlayer.State.Ended && clip.isAtOrPastEffectiveEnd()
        runInterruptible(Dispatchers.IO) {
            if (rewindFromEof) {
                seekClipUs(clip, 0L)
            }
            clip.start()
        }
        state.emit(PlaybackPlayer.State.Playing)
        startProgressJob()
    }

    override suspend fun seekTo(position: Long) {
        val uri = currentUri ?: return
        suppressEndedAfterUserSeekUntilMs = System.currentTimeMillis() + 2_500L
        currentPosition = position.coerceAtLeast(0L)
        val clip = currentClip
        if (clip != null) {
            runInterruptible(Dispatchers.IO) {
                seekClipUs(clip, msToUs(currentPosition))
            }
            if (playWhenReady) {
                state.emit(PlaybackPlayer.State.Playing)
                startProgressJob()
            }
            emitProgressSnapshot(clip)
        } else {
            _progress.emit(
                PlaybackPlayer.Progress(currentPosition, currentDurationMs),
            )
            if (playWhenReady) {
                openClipForUri(uri, currentPosition)
                currentClip?.let { c ->
                    runInterruptible(Dispatchers.IO) { c.start() }
                    state.emit(PlaybackPlayer.State.Playing)
                    startProgressJob()
                    emitProgressSnapshot(c)
                }
            }
        }
    }

    override suspend fun setMediaItem(item: MediaItemWrapper, position: Long) {
        suppressEndedAfterUserSeekUntilMs = 0L
        stopProgressJob()
        closeClip()
        currentUri = item.uri
        currentPosition = position.coerceAtLeast(0L)
        val file = File(item.uri)
        if (!file.exists()) {
            Log.w("DesktopPlayer") { "File not found: ${item.uri}" }
            state.emit(PlaybackPlayer.State.Ended)
            return
        }
        val tagDurationMs = readTagDurationMs(file)
        try {
            val clip = runInterruptible(Dispatchers.IO) {
                AudioSystem.getClip().also { c ->
                    openPcmStreamForClip(file).use { ais -> c.open(ais) }
                }
            }
            currentClip = clip
            val lenUs = clip.microsecondLength
            currentDurationMs = when {
                lenUs > 0L -> lenUs / 1000L
                tagDurationMs > 0L -> tagDurationMs
                else -> 0L
            }
            runInterruptible(Dispatchers.IO) {
                seekClipUs(clip, msToUs(currentPosition))
            }
            if (playWhenReady) {
                runInterruptible(Dispatchers.IO) { clip.start() }
                state.emit(PlaybackPlayer.State.Playing)
                startProgressJob()
            } else {
                state.emit(PlaybackPlayer.State.Paused)
            }
            emitProgressSnapshot(clip)
        } catch (e: Exception) {
            Log.e("DesktopPlayer") { "Could not open clip: ${e.message}" }
            currentClip = null
            state.emit(PlaybackPlayer.State.Ended)
        }
    }

    private suspend fun stopProgressJob() {
        val job = progressJob
        progressJob = null
        job?.cancelAndJoin()
    }

    private suspend fun closeClip() {
        val clip = currentClip
        currentClip = null
        if (clip != null) {
            runInterruptible(Dispatchers.IO) {
                try {
                    clip.stop()
                } catch (_: Exception) {
                }
                try {
                    clip.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun readTagDurationMs(file: File): Long =
        try {
            AudioFileIO.read(file).audioHeader.trackLength * 1000L
        } catch (_: Exception) {
            0L
        }

    /**
     * [Clip.open] requires a fully specified [AudioFormat]; the JDK MP3 provider often leaves
     * frame size at NOT_SPECIFIED. MP3SPI ([MpegAudioFileReader]) plus PCM decode fixes that.
     */
    private fun openPcmStreamForClip(file: File): AudioInputStream {
        val raw = openRawAudioInputStream(file)
        val rawFormat = raw.format
        val channels = rawFormat.channels.takeIf { it > 0 } ?: 2
        val sampleRate = rawFormat.sampleRate.takeIf { it > 0f } ?: 44100f
        val pcmFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            channels,
            channels * 2,
            sampleRate,
            false,
        )
        val useRaw = rawFormat.encoding == AudioFormat.Encoding.PCM_SIGNED &&
                rawFormat.sampleSizeInBits == 16 &&
                rawFormat.frameSize > 0
        val pcm = if (useRaw) raw else AudioSystem.getAudioInputStream(pcmFormat, raw)
        return wrapEqualizer(pcm)
    }

    private fun wrapEqualizer(stream: AudioInputStream): AudioInputStream {
        val f = stream.format
        if (f.encoding != AudioFormat.Encoding.PCM_SIGNED || f.sampleSizeInBits != 16) return stream
        val ch = f.channels
        if (ch <= 0) return stream
        val sr = f.sampleRate
        if (sr <= 0f) return stream
        val gains = equalizerGains.copyOf()
        return EqualizingPcmAudioInputStream(stream, ch, sr, gains, DesktopEqualizerPresets.bandCentersHz)
    }

    private fun openRawAudioInputStream(file: File): AudioInputStream {
        val isMp3 = file.extension.equals("mp3", ignoreCase = true)
        return if (isMp3) {
            try {
                MpegAudioFileReader().getAudioInputStream(file)
            } catch (_: UnsupportedAudioFileException) {
                AudioSystem.getAudioInputStream(file)
            }
        } else {
            AudioSystem.getAudioInputStream(file)
        }
    }

    private suspend fun openClipForUri(uri: String, startMs: Long) {
        val file = File(uri)
        if (!file.exists()) {
            Log.w("DesktopPlayer") { "File not found: $uri" }
            return
        }
        val tagDurationMs = readTagDurationMs(file)
        try {
            val clip = runInterruptible(Dispatchers.IO) {
                AudioSystem.getClip().also { c ->
                    openPcmStreamForClip(file).use { ais -> c.open(ais) }
                }
            }
            currentClip = clip
            val lenUs = clip.microsecondLength
            currentDurationMs = when {
                lenUs > 0L -> lenUs / 1000L
                tagDurationMs > 0L -> tagDurationMs
                else -> 0L
            }
            runInterruptible(Dispatchers.IO) {
                seekClipUs(clip, msToUs(startMs.coerceAtLeast(0L)))
            }
            currentPosition = startMs
        } catch (e: Exception) {
            Log.e("DesktopPlayer") { "Could not open clip: ${e.message}" }
            currentClip = null
        }
    }

    private fun msToUs(ms: Long): Long = ms.coerceAtLeast(0L) * 1000L

    private fun suppressTrackEndedFor(ms: Long) {
        suppressTrackEndedUntilMs = System.currentTimeMillis() + ms
    }

    /** After MP3→PCM decode, [Clip.getMicrosecondLength] is often invalid; fall back to tag duration. */
    private fun effectiveMicrosecondLength(clip: Clip): Long {
        val fromClip = clip.microsecondLength
        return when {
            fromClip > 0L -> fromClip
            currentDurationMs > 0L -> currentDurationMs * 1000L
            else -> 0L
        }
    }

    private fun Clip.isAtOrPastEffectiveEnd(): Boolean {
        val eff = effectiveMicrosecondLength(this)
        if (eff <= 0L) return false
        return microsecondPosition + 300_000L >= eff
    }

    /**
     * Clip may require [Clip.stop] before [Clip.setMicrosecondPosition] while the line is active.
     * After a scrub to EOF the line is stopped ([isRunning] false) but [playWhenReady] stays true — we still
     * must [Clip.start] so dragging the slider back resumes audio without an extra play tap.
     */
    private fun seekClipUs(clip: Clip, positionUs: Long) {
        suppressTrackEndedFor(600L)
        val wasRunning = clip.isRunning
        if (wasRunning) {
            try {
                clip.stop()
            } catch (_: Exception) {
            }
        }
        val lenFromClip = clip.microsecondLength
        val effLen = effectiveMicrosecondLength(clip)
        val target = when {
            lenFromClip > 0L -> positionUs.coerceIn(0L, (lenFromClip - 1L).coerceAtLeast(0L))
            effLen > 0L -> positionUs.coerceIn(0L, (effLen - 1L).coerceAtLeast(0L))
            else -> positionUs.coerceAtLeast(0L)
        }
        clip.microsecondPosition = target
        currentPosition = target / 1000L
        if (playWhenReady) {
            try {
                clip.start()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun emitProgressSnapshot(clip: Clip) {
        val (posMs, durMs) = runInterruptible(Dispatchers.IO) {
            val lenUs = clip.microsecondLength
            val durMs = when {
                lenUs > 0L -> lenUs / 1000L
                currentDurationMs > 0L -> currentDurationMs
                else -> 0L
            }
            val posMs = clip.microsecondPosition / 1000L
            Pair(posMs, durMs)
        }
        currentPosition = posMs
        _progress.emit(
            PlaybackPlayer.Progress(
                posMs.coerceAtMost(durMs.coerceAtLeast(1L)),
                durMs,
            ),
        )
    }

    private fun startProgressJob() {
        progressJob?.cancel()
        progressJob = playbackScope.launch {
            while (isActive) {
                val clip = currentClip ?: break
                val ended = runInterruptible(Dispatchers.IO) {
                    val lenUsFromClip = clip.microsecondLength
                    val effLenUs = effectiveMicrosecondLength(clip)
                    val posUs = clip.microsecondPosition
                    val durMs = when {
                        lenUsFromClip > 0L -> lenUsFromClip / 1000L
                        currentDurationMs > 0L -> currentDurationMs
                        else -> 0L
                    }
                    val posMs = posUs / 1000L
                    currentPosition = posMs
                    _progress.value = PlaybackPlayer.Progress(
                        posMs.coerceAtMost(durMs.coerceAtLeast(1L)),
                        durMs,
                    )
                    if (!playWhenReady || clip.isRunning) return@runInterruptible false
                    if (System.currentTimeMillis() < suppressTrackEndedUntilMs) return@runInterruptible false
                    if (System.currentTimeMillis() < suppressEndedAfterUserSeekUntilMs) return@runInterruptible false
                    if (effLenUs <= 0L) return@runInterruptible false
                    val endSlackUs = minOf(2_000_000L, effLenUs / 8).coerceAtLeast(150_000L)
                    posUs + endSlackUs >= effLenUs
                }
                if (ended) {
                    state.emit(PlaybackPlayer.State.Ended)
                    break
                }
                delay(100)
            }
        }
    }

    suspend fun applyEqualizerPreset(builtInIndex: Int) {
        val i = builtInIndex.coerceIn(0, DesktopEqualizerPresets.names.lastIndex)
        val newGains = DesktopEqualizerPresets.gainsForPreset(i)
        val uri = currentUri ?: run {
            equalizerGains = newGains
            return
        }
        if (newGains.contentEquals(equalizerGains) && currentClip != null) return
        equalizerGains = newGains
        val savedPosition = currentPosition
        val wasPlaying = playWhenReady
        stopProgressJob()
        closeClip()
        openClipForUri(uri, savedPosition)
        val clip = currentClip
        if (clip != null) {
            if (wasPlaying) {
                runInterruptible(Dispatchers.IO) { clip.start() }
                state.emit(PlaybackPlayer.State.Playing)
                startProgressJob()
            } else {
                state.emit(PlaybackPlayer.State.Paused)
            }
            emitProgressSnapshot(clip)
        }
    }

    suspend fun applyEqualizerCustomGains(gainsDb: FloatArray) {
        if (gainsDb.size != DesktopEqualizerPresets.bandCentersHz.size) return
        val next = gainsDb.map { it.coerceIn(DesktopEqualizerPresets.GAIN_DB_MIN, DesktopEqualizerPresets.GAIN_DB_MAX) }.toFloatArray()
        val uri = currentUri ?: run {
            equalizerGains = next
            return
        }
        if (next.contentEquals(equalizerGains) && currentClip != null) return
        equalizerGains = next
        val savedPosition = currentPosition
        val wasPlaying = playWhenReady
        stopProgressJob()
        closeClip()
        openClipForUri(uri, savedPosition)
        val clip = currentClip
        if (clip != null) {
            if (wasPlaying) {
                runInterruptible(Dispatchers.IO) { clip.start() }
                state.emit(PlaybackPlayer.State.Playing)
                startProgressJob()
            } else {
                state.emit(PlaybackPlayer.State.Paused)
            }
            emitProgressSnapshot(clip)
        }
    }
}
