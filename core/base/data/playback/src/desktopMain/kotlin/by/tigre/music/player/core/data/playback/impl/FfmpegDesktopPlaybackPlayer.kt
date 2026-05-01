package by.tigre.music.player.core.data.playback.impl

import by.tigre.music.player.core.data.playback.AppPlaybackVolume
import by.tigre.music.player.core.data.playback.MediaItemWrapper
import by.tigre.music.player.core.data.playback.PlaybackEqualizer
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.data.playback.impl.dsp.DesktopEqualizerPresets
import by.tigre.music.player.core.data.playback.impl.dsp.Pcm16EqualizerProcessor
import by.tigre.music.player.core.data.playback.prefs.EqualizerPreferences
import by.tigre.music.player.core.data.playback.prefs.PlaybackVolumePreferences
import by.tigre.music.player.core.data.playback.prefs.alignGainsToBandCount
import by.tigre.music.player.logger.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bytedeco.javacpp.Loader
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread
import kotlin.math.min

/**
 * Desktop playback via FFmpeg (JavaCV / Bytedeco). PCM S16 → [Pcm16EqualizerProcessor] → [SourceDataLine].
 * See [FFMPEG_DESKTOP_PLAYBACK_PLAN.md] in this module.
 */
internal class FfmpegDesktopPlaybackPlayer private constructor(
    private val equalizerPrefs: EqualizerPreferences,
    private val volumePrefs: PlaybackVolumePreferences,
) : PlaybackPlayer, PlaybackEqualizer, AppPlaybackVolume {

    private val playbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _playbackVolume = MutableStateFlow(volumePrefs.load())
    override val playbackVolume: StateFlow<Float> = _playbackVolume.asStateFlow()

    override fun setPlaybackVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        _playbackVolume.value = v
        volumePrefs.save(v)
    }

    private val grabberLock = Any()

    @Volatile
    private var grabber: FFmpegFrameGrabber? = null

    @Volatile
    private var audioLine: SourceDataLine? = null

    @Volatile
    private var pcmEqualizer: Pcm16EqualizerProcessor? = null

    @Volatile
    private var runDecode = false

    @Volatile
    private var decodeThread: Thread? = null

    @Volatile
    private var playWhenReady = false

    @Volatile
    private var currentUri: String? = null

    @Volatile
    private var currentDurationMs: Long = 0L

    @Volatile
    private var lastPositionMs: Long = 0L

    @Volatile
    private var suppressEndedAfterUserSeekUntilMs: Long = 0L

    private var progressJob: Job? = null

    private val builtInPresetCount = DesktopEqualizerPresets.names.size
    private val customPresetIdx get() = builtInPresetCount

    private var currentEqGains: FloatArray = floatArrayOf()

    private val _presetNames = MutableStateFlow(DesktopEqualizerPresets.names + "Custom")
    override val presetNames = _presetNames.asStateFlow()

    private val _available = MutableStateFlow(true)
    override val isAvailable = _available.asStateFlow()

    private val _selectedPreset = MutableStateFlow(0)
    override val selectedPresetIndex = _selectedPreset.asStateFlow()

    private val _bandCenterHz = MutableStateFlow(DesktopEqualizerPresets.bandCentersHz.toList())
    override val bandCenterHz = _bandCenterHz.asStateFlow()

    private val _bandGainDb = MutableStateFlow<List<Float>>(emptyList())
    override val bandGainDb = _bandGainDb.asStateFlow()

    private val _builtInPresetBandGainsDb =
        MutableStateFlow(DesktopEqualizerPresets.allBuiltInBandGainsDb())
    override val builtInPresetBandGainsDb = _builtInPresetBandGainsDb.asStateFlow()

    private val _customPresetIndex = MutableStateFlow(customPresetIdx)
    override val customPresetIndex = _customPresetIndex.asStateFlow()

    private val _bandGainRangeDb =
        MutableStateFlow(DesktopEqualizerPresets.GAIN_DB_MIN to DesktopEqualizerPresets.GAIN_DB_MAX)
    override val bandGainRangeDb = _bandGainRangeDb.asStateFlow()

    init {
        restoreEqualizerFromPreferences()
    }

    private fun restoreEqualizerFromPreferences() {
        val bandN = DesktopEqualizerPresets.bandCentersHz.size
        val savedIdx = equalizerPrefs.loadSelectedPresetIndex(0).coerceIn(0, customPresetIdx)
        if (savedIdx < builtInPresetCount) {
            currentEqGains = DesktopEqualizerPresets.gainsForPreset(savedIdx)
            _selectedPreset.value = savedIdx
        } else {
            currentEqGains =
                alignGainsToBandCount(equalizerPrefs.loadCustomBandGainsDb(), bandN).toFloatArray()
            _selectedPreset.value = customPresetIdx
        }
        _bandGainDb.value = currentEqGains.toList()
    }

    override val state = MutableStateFlow(PlaybackPlayer.State.Idle)
    private val _progress = MutableStateFlow(PlaybackPlayer.Progress(0, 0))
    override val progress: Flow<PlaybackPlayer.Progress> = _progress

    private fun startDecodeThreadLocked() {
        if (decodeThread?.isAlive == true) return
        runDecode = true
        decodeThread =
            thread(start = true, isDaemon = true, name = "ffmpeg-decode") {
                decodeLoop()
            }
    }

    private fun decodeLoop() {
        while (runDecode && !Thread.currentThread().isInterrupted) {
            if (!playWhenReady) {
                Thread.sleep(20)
                continue
            }
            synchronized(grabberLock) { grabber } ?: break
            val line = synchronized(grabberLock) { audioLine } ?: break
            val eq = synchronized(grabberLock) { pcmEqualizer }

            val frame: Frame?
            try {
                frame = synchronized(grabberLock) { grabber?.grabSamples() }
            } catch (_: Exception) {
                playbackScope.launch { onDecodeFailureOrEof() }
                break
            }

            if (frame == null) {
                playbackScope.launch { onDecodeFailureOrEof() }
                break
            }

            val samples = frame.samples
            if (samples == null || samples.isEmpty()) {
                continue
            }

            val pcm = ffmpegFrameToInterleavedS16Le(frame) ?: continue
            eq?.processInterleavedPcmS16(pcm, 0, pcm.size, bigEndian = false)
            scaleInterleavedPcmS16Le(pcm, 0, pcm.size, _playbackVolume.value)

            var offset = 0
            while (offset < pcm.size && runDecode && playWhenReady) {
                if (Thread.currentThread().isInterrupted) {
                    return
                }
                val n = try {
                    line.write(pcm, offset, pcm.size - offset)
                } catch (_: Exception) {
                    break
                }
                if (n <= 0) {
                    break
                }
                offset += n
            }

            updateProgressFromFrame(frame)
        }
    }

    private suspend fun onDecodeFailureOrEof() {
        if (suppressEndedAfterUserSeekUntilMs != 0L &&
            System.currentTimeMillis() < suppressEndedAfterUserSeekUntilMs
        ) {
            return
        }
        if (playWhenReady) {
            stopProgressJob()
            synchronized(grabberLock) {
                runCatching { audioLine?.stop() }
            }
            state.emit(PlaybackPlayer.State.Ended)
        }
    }

    private fun updateProgressFromFrame(frame: Frame) {
        val tsUs = when {
            frame.timestamp > 0L -> frame.timestamp
            else ->
                synchronized(grabberLock) {
                    grabber?.timestamp ?: 0L
                }
        }
        if (tsUs > 0L) {
            lastPositionMs = (tsUs / 1000L).coerceAtLeast(0L)
        }
    }

    override fun selectPreset(index: Int) {
        if (index !in 0..customPresetIdx) return
        _selectedPreset.value = index
        equalizerPrefs.saveSelectedPresetIndex(index)
        synchronized(grabberLock) {
            val eq = pcmEqualizer
            if (index < builtInPresetCount) {
                currentEqGains = DesktopEqualizerPresets.gainsForPreset(index)
                eq?.setPreset(index)
            } else {
                val bandN = DesktopEqualizerPresets.bandCentersHz.size
                currentEqGains =
                    alignGainsToBandCount(equalizerPrefs.loadCustomBandGainsDb(), bandN).toFloatArray()
                eq?.setGains(currentEqGains.copyOf())
                equalizerPrefs.saveCustomBandGainsDb(currentEqGains.toList())
            }
        }
        _bandGainDb.value = currentEqGains.toList()
    }

    override fun setBandGainDb(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in currentEqGains.indices) return
        currentEqGains[bandIndex] =
            gainDb.coerceIn(DesktopEqualizerPresets.GAIN_DB_MIN, DesktopEqualizerPresets.GAIN_DB_MAX)
        _selectedPreset.value = customPresetIdx
        equalizerPrefs.saveSelectedPresetIndex(customPresetIdx)
        equalizerPrefs.saveCustomBandGainsDb(currentEqGains.toList())
        synchronized(grabberLock) {
            pcmEqualizer?.setGains(currentEqGains.copyOf())
        }
        _bandGainDb.value = currentEqGains.toList()
    }

    override suspend fun stop() {
        playWhenReady = false
        suppressEndedAfterUserSeekUntilMs = 0L
        stopProgressJob()
        stopDecodeJoin()
        synchronized(grabberLock) {
            runCatching { audioLine?.stop() }
            runCatching { audioLine?.close() }
            audioLine = null
            runCatching { grabber?.stop() }
            runCatching { grabber?.release() }
            grabber = null
            pcmEqualizer = null
        }
        currentUri = null
        currentDurationMs = 0L
        lastPositionMs = 0L
        state.emit(PlaybackPlayer.State.Idle)
        _progress.emit(PlaybackPlayer.Progress(0, 0))
    }

    override suspend fun pause() {
        playWhenReady = false
        synchronized(grabberLock) {
            runCatching { audioLine?.stop() }
        }
        state.emit(PlaybackPlayer.State.Paused)
    }

    override suspend fun resume() {
        if (currentUri == null) return
        playWhenReady = true
        synchronized(grabberLock) {
            runCatching { audioLine?.start() }
        }
        state.emit(PlaybackPlayer.State.Playing)
        startDecodeThreadLocked()
        startProgressJob()
    }

    override suspend fun seekTo(position: Long) {
        if (currentUri == null) return
        suppressEndedAfterUserSeekUntilMs = System.currentTimeMillis() + 2_500L
        val p = position.coerceAtLeast(0L)
        val len = synchronized(grabberLock) {
            runCatching {
                grabber?.setTimestamp(p * 1000L)
            }
            val g = grabber
            val fromGrabber =
                if (g != null && g.lengthInTime > 0L) {
                    g.lengthInTime / 1000L
                } else {
                    0L
                }
            if (fromGrabber > 0L) currentDurationMs = fromGrabber
            currentDurationMs
        }
        lastPositionMs = p.coerceAtMost(len.coerceAtLeast(1L))
        _progress.emit(
            PlaybackPlayer.Progress(
                lastPositionMs,
                len,
            ),
        )
        if (playWhenReady) {
            state.emit(PlaybackPlayer.State.Playing)
            startProgressJob()
        }
    }

    override suspend fun setMediaItem(item: MediaItemWrapper, position: Long) {
        suppressEndedAfterUserSeekUntilMs = 0L
        stopProgressJob()
        stopDecodeJoin()

        val file = File(item.uri)
        currentUri = item.uri
        if (!file.exists()) {
            Log.w("FfmpegDesktopPlayer") { "File not found: ${item.uri}" }
            state.emit(PlaybackPlayer.State.Ended)
            return
        }

        val tagMs = readTagDurationMs(file)
        var durationMs = tagMs

        var mediaOpened = false
        synchronized(grabberLock) {
            runCatching { audioLine?.stop() }
            runCatching { audioLine?.close() }
            audioLine = null
            runCatching { grabber?.stop() }
            runCatching { grabber?.release() }
            grabber = null
            pcmEqualizer = null

            val g =
                try {
                    FFmpegFrameGrabber(file).apply {
                        start()
                    }
                } catch (e: Exception) {
                    Log.e("FfmpegDesktopPlayer") { "Could not open media: ${e.message}" }
                    null
                }

            if (g == null) {
                state.value = PlaybackPlayer.State.Ended
                return@synchronized
            }

            grabber = g

            if (g.lengthInTime > 0L) {
                durationMs = g.lengthInTime / 1000L
            }
            currentDurationMs = durationMs

            val ch = g.audioChannels.coerceAtLeast(1)
            val sr = g.sampleRate.coerceAtLeast(1)

            val format =
                AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sr.toFloat(),
                    16,
                    ch,
                    ch * 2,
                    sr.toFloat(),
                    false,
                )
            val info = DataLine.Info(SourceDataLine::class.java, format)
            if (!AudioSystem.isLineSupported(info)) {
                Log.e("FfmpegDesktopPlayer") { "Audio line not supported: $format" }
                runCatching { g.stop() }
                runCatching { g.release() }
                grabber = null
                state.value = PlaybackPlayer.State.Ended
                return@synchronized
            }

            val line = AudioSystem.getLine(info) as SourceDataLine
            line.open(format)
            audioLine = line

            pcmEqualizer =
                Pcm16EqualizerProcessor.forGains(
                    channels = ch,
                    sampleRate = sr.toFloat(),
                    gainsDb = currentEqGains.copyOf(),
                    centersHz = DesktopEqualizerPresets.bandCentersHz,
                )

            val startMs = position.coerceAtLeast(0L)
            runCatching {
                g.setTimestamp(startMs * 1000L)
            }
            lastPositionMs = startMs.coerceAtMost(durationMs.coerceAtLeast(1L))

            _progress.value = PlaybackPlayer.Progress(lastPositionMs, durationMs)
            mediaOpened = true
        }

        if (!mediaOpened) {
            return
        }

        if (playWhenReady) {
            synchronized(grabberLock) {
                runCatching { audioLine?.start() }
            }
            state.emit(PlaybackPlayer.State.Playing)
            startDecodeThreadLocked()
            startProgressJob()
        } else {
            state.emit(PlaybackPlayer.State.Paused)
        }
    }

    private suspend fun stopDecodeJoin() =
        withContext(Dispatchers.IO) {
            runDecode = false
            decodeThread?.interrupt()
            decodeThread?.join(5000)
            decodeThread = null
        }

    private suspend fun stopProgressJob() {
        val job = progressJob
        progressJob = null
        job?.cancelAndJoin()
    }

    private fun startProgressJob() {
        progressJob?.cancel()
        progressJob =
            playbackScope.launch {
                while (isActive && currentUri != null) {
                    val len = currentDurationMs
                    val pos = lastPositionMs.coerceAtMost(len.coerceAtLeast(1L))
                    _progress.emit(PlaybackPlayer.Progress(pos, len))
                    delay(100)
                }
            }
    }

    private fun readTagDurationMs(file: File): Long =
        try {
            AudioFileIO.read(file).audioHeader.trackLength * 1000L
        } catch (_: Exception) {
            0L
        }

    private fun registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                runCatching {
                    runBlockingStopForShutdown()
                }
            },
        )
    }

    private fun runBlockingStopForShutdown() {
        runDecode = false
        decodeThread?.interrupt()
        decodeThread?.join(3000)
        synchronized(grabberLock) {
            runCatching { audioLine?.stop() }
            runCatching { audioLine?.close() }
            audioLine = null
            runCatching { grabber?.stop() }
            runCatching { grabber?.release() }
            grabber = null
        }
    }

    companion object {

        fun tryCreate(
            equalizerPrefs: EqualizerPreferences,
            volumePrefs: PlaybackVolumePreferences,
        ): FfmpegDesktopPlaybackPlayer? =
            runCatching {
                Loader.load(org.bytedeco.ffmpeg.global.avutil::class.java)
                FfmpegDesktopPlaybackPlayer(equalizerPrefs, volumePrefs).also { it.registerShutdownHook() }
            }.getOrElse { e ->
                Log.w("FfmpegDesktopPlayer") {
                    "FFmpeg (JavaCV) not available — ${e.message}"
                }
                null
            }
    }
}

/** In-place linear gain on little-endian interleaved S16 PCM. */
private fun scaleInterleavedPcmS16Le(pcm: ByteArray, offset: Int, length: Int, gainLinear: Float) {
    if (gainLinear >= 0.999f) return
    val g = gainLinear.coerceIn(0f, 1f)
    var i = offset
    val end = offset + length
    while (i + 1 < end) {
        var s = (pcm[i].toInt() and 0xff) or ((pcm[i + 1].toInt() and 0xff) shl 8)
        if (s >= 0x8000) s -= 0x10000
        val scaled = (s * g).toInt().coerceIn(-32768, 32767)
        pcm[i] = (scaled and 0xff).toByte()
        pcm[i + 1] = ((scaled shr 8) and 0xff).toByte()
        i += 2
    }
}

/** Converts one FFmpeg audio [Frame] to little-endian interleaved PCM S16. */
private fun ffmpegFrameToInterleavedS16Le(frame: Frame): ByteArray? {
    val ch = frame.audioChannels
    if (ch <= 0) return null
    val bufs = frame.samples ?: return null
    val b0 = bufs[0] ?: return null

    if (b0 is ShortBuffer && bufs.size >= ch && ch > 1) {
        val first = bufs[0] as ShortBuffer
        val n = first.remaining()
        if (n <= 0) return null
        var planar = true
        for (c in 0 until ch) {
            val sb = bufs[c] as? ShortBuffer ?: return null
            if (sb.remaining() != n) {
                planar = false
                break
            }
        }
        if (planar) {
            val dup = Array(ch) { (bufs[it] as ShortBuffer).duplicate() }
            val out = ByteArray(n * ch * 2)
            var o = 0
            repeat(n) {
                for (c in 0 until ch) {
                    val s = dup[c].get()
                    out[o++] = (s.toInt() and 0xff).toByte()
                    out[o++] = ((s.toInt() shr 8) and 0xff).toByte()
                }
            }
            return out
        }
    }

    if (b0 is ShortBuffer) {
        val sb = b0.duplicate()
        val n = sb.remaining()
        if (n <= 0) return null
        val out = ByteArray(n * 2)
        var o = 0
        while (sb.hasRemaining()) {
            val s = sb.get()
            out[o++] = (s.toInt() and 0xff).toByte()
            out[o++] = ((s.toInt() shr 8) and 0xff).toByte()
        }
        return out
    }

    if (b0 is FloatBuffer) {
        if (ch == 1) {
            val fb = b0.duplicate()
            val n = fb.remaining()
            if (n <= 0) return null
            val out = ByteArray(n * 2)
            var o = 0
            while (fb.hasRemaining()) {
                val sample =
                    (fb.get().toDouble().coerceIn(-1.0, 1.0) * 32767.0)
                        .toInt()
                        .coerceIn(-32768, 32767)
                out[o++] = (sample and 0xff).toByte()
                out[o++] = ((sample shr 8) and 0xff).toByte()
            }
            return out
        }
        if (bufs.size >= 2 && bufs[1] is FloatBuffer) {
            val l = (bufs[0] as FloatBuffer).duplicate()
            val r = (bufs[1] as FloatBuffer).duplicate()
            val n = min(l.remaining(), r.remaining())
            if (n <= 0) return null
            val out = ByteArray(n * 4)
            var o = 0
            repeat(n) {
                for (fb in arrayOf(l, r)) {
                    val sample =
                        (fb.get().toDouble().coerceIn(-1.0, 1.0) * 32767.0)
                            .toInt()
                            .coerceIn(-32768, 32767)
                    out[o++] = (sample and 0xff).toByte()
                    out[o++] = ((sample shr 8) and 0xff).toByte()
                }
            }
            return out
        }
    }

    return null
}
