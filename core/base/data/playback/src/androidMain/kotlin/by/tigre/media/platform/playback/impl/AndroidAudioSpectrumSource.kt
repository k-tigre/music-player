package by.tigre.media.platform.playback.impl

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import by.tigre.logger.Log
import by.tigre.media.platform.playback.AndroidPlaybackPlayer
import by.tigre.media.platform.playback.AudioSpectrumSource
import by.tigre.media.platform.playback.SpectrumFrame
import by.tigre.media.platform.playback.VisualizerProcessing
import by.tigre.media.platform.playback.prefs.VisualizerPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Dual processing (debug-switchable via [VisualizerPreferences.processing]):
 * - [VisualizerProcessing.Pretty]: ComposeCircle shape, then × track dynamics (Tee).
 * - [VisualizerProcessing.Realistic]: spectrum shape × same track dynamics.
 * Quiet passages collapse via [dynamicsGain] so bars don't look "full" when the mix is soft.
 *
 * Frames are delayed to match speaker playout: Tee/Visualizer see PCM before the
 * AudioTrack + device buffer, so without delay the viz leads the audible sound.
 */
internal class AndroidAudioSpectrumSource(
    private val context: Context,
    private val androidPlaybackPlayer: AndroidPlaybackPlayer,
    private val trackLevelMeter: TrackPcmLevelMeter,
    private val visualizerPreferences: VisualizerPreferences,
) : AudioSpectrumSource {

    private val _frames = MutableStateFlow<SpectrumFrame?>(null)
    override val frames: StateFlow<SpectrumFrame?> = _frames.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var visualizer: Visualizer? = null
    private var enabled: Boolean = false
    private var sessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var prevRms: Float = 0f
    private var beatPulse: Float = 0f
    private var waveformRms: Float = 0f
    private var trackTeePeak: Float = 0f
    private var dynamicsEnv: Float = 0f
    private var lastSamplingRateHz: Int = 44_100
    private var silenceFrames: Int = 0
    private var outputDelayMs: Long = DEFAULT_OUTPUT_DELAY_MS
    private val pendingFrames = ArrayDeque<PendingFrame>()
    private val flushRunnable = Runnable { flushPendingFrames() }

    private val exoPlayer: ExoPlayer
        get() = androidPlaybackPlayer.player as ExoPlayer

    private val listener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            sessionId = audioSessionId
            if (enabled) attach(audioSessionId)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            clearOutput(resetTrackPeak = true)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) clearOutput(resetTrackPeak = false)
        }
    }

    init {
        exoPlayer.addListener(listener)
        sessionId = exoPlayer.audioSessionId
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (enabled) {
            attach(sessionId)
        } else {
            releaseVisualizer()
            clearOutput(resetTrackPeak = false)
        }
    }

    override fun release() {
        enabled = false
        exoPlayer.removeListener(listener)
        releaseVisualizer()
        clearOutput(resetTrackPeak = true)
    }

    private fun clearOutput(resetTrackPeak: Boolean) {
        mainHandler.removeCallbacks(flushRunnable)
        synchronized(pendingFrames) { pendingFrames.clear() }
        waveformRms = 0f
        prevRms = 0f
        beatPulse = 0f
        dynamicsEnv = 0f
        silenceFrames = 0
        trackLevelMeter.reset()
        if (resetTrackPeak) trackTeePeak = 0f
        _frames.value = silentFrame()
    }

    private fun attach(audioSessionId: Int) {
        releaseVisualizer()
        clearOutput(resetTrackPeak = false)
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        try {
            outputDelayMs = estimateOutputDelayMs()
            val range = Visualizer.getCaptureSizeRange()
            var captureSize = 1024
            while (captureSize > range[1]) captureSize /= 2
            while (captureSize < range[0]) captureSize *= 2
            val rate = Visualizer.getMaxCaptureRate().coerceAtMost(20_000)
            val viz = Visualizer(audioSessionId)
            viz.captureSize = captureSize
            // NORMALIZED: spectrum available even when device volume is 0.
            viz.scalingMode = Visualizer.SCALING_MODE_NORMALIZED
            viz.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int,
                    ) {
                        if (waveform == null || waveform.isEmpty()) return
                        var sumSq = 0.0
                        for (s in waveform) {
                            val centered = (s.toInt() and 0xFF) - 128
                            sumSq += centered * centered
                        }
                        waveformRms = sqrt(sumSq / waveform.size).toFloat() / 128f
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int,
                    ) {
                        if (fft == null || fft.size < 4) return
                        val rateHz = if (samplingRate > 1_000_000) samplingRate / 1000 else samplingRate
                        if (rateHz > 0) lastSamplingRateHz = rateHz
                        val captureSize = fft.size
                        val processing = visualizerPreferences.processing.value
                        val frame = when (processing) {
                            VisualizerProcessing.Pretty -> processPretty(fft, captureSize, lastSamplingRateHz)
                            VisualizerProcessing.Realistic -> processRealistic(fft, captureSize, lastSamplingRateHz)
                        }
                        enqueueFrame(frame)
                    }
                },
                rate,
                true,
                true,
            )
            viz.enabled = true
            visualizer = viz
            Log.d(TAG) {
                "Visualizer attached capture=$captureSize session=$audioSessionId delay=${outputDelayMs}ms"
            }
        } catch (e: Exception) {
            Log.w(TAG) { "Visualizer unavailable: ${e.message}" }
            releaseVisualizer()
        }
    }

    /** Hold frames so UI lines up with speaker output (capture is ahead of DAC). */
    private fun enqueueFrame(frame: SpectrumFrame) {
        val dueAt = SystemClock.uptimeMillis() + outputDelayMs
        val earliestDue: Long
        synchronized(pendingFrames) {
            pendingFrames.addLast(PendingFrame(dueAt, frame))
            while (pendingFrames.size > MAX_PENDING_FRAMES) pendingFrames.removeFirst()
            earliestDue = pendingFrames.first().dueAtUptimeMs
        }
        // Always (re)schedule for the earliest due frame — never only the newest,
        // or continuous capture keeps pushing flush into the future and nothing draws.
        mainHandler.removeCallbacks(flushRunnable)
        mainHandler.postAtTime(flushRunnable, earliestDue)
    }

    private fun flushPendingFrames() {
        val now = SystemClock.uptimeMillis()
        var latest: SpectrumFrame? = null
        var nextDue: Long? = null
        synchronized(pendingFrames) {
            while (pendingFrames.isNotEmpty() && pendingFrames.first().dueAtUptimeMs <= now) {
                latest = pendingFrames.removeFirst().frame
            }
            if (pendingFrames.isNotEmpty()) {
                nextDue = pendingFrames.first().dueAtUptimeMs
            }
        }
        if (latest != null) {
            _frames.value = latest
        }
        nextDue?.let { mainHandler.postAtTime(flushRunnable, it) }
    }

    /**
     * Hardware output latency (hidden AudioManager API) + ExoPlayer AudioTrack buffer slack.
     * Visualizer/Tee observe PCM before it leaves the device buffer.
     */
    private fun estimateOutputDelayMs(): Long {
        val hwMs = platformOutputLatencyMs()
        val delay = (hwMs + EXOPLAYER_BUFFER_COMPENSATION_MS).coerceIn(MIN_OUTPUT_DELAY_MS, MAX_OUTPUT_DELAY_MS)
        Log.d(TAG) { "output delay estimate=${delay}ms (hw=$hwMs + buffer=$EXOPLAYER_BUFFER_COMPENSATION_MS)" }
        return delay
    }

    private fun platformOutputLatencyMs(): Long {
        return try {
            val method = AudioManager::class.java.getMethod("getOutputLatency", Int::class.javaPrimitiveType)
            val ms = method.invoke(audioManager, AudioManager.STREAM_MUSIC) as Int
            ms.toLong().coerceAtLeast(0L)
        } catch (_: Exception) {
            40L
        }
    }

    private data class PendingFrame(val dueAtUptimeMs: Long, val frame: SpectrumFrame)

    // --- Pretty: ComposeCircle shape, then absolute track dynamics ---

    private fun processPretty(fft: ByteArray, captureSize: Int, sampleRateHz: Int): SpectrumFrame {
        if (!exoPlayer.isPlaying) return silentOrDecay()

        val gain = dynamicsGain()
        if (gain <= 0.001f) return silentOrDecay()

        val mags = fftMagnitudes(fft)
        if (mags.isEmpty()) return silentFrame()

        val filtered = filterFrequency(mags, sampleRateHz, captureSize, MIN_HZ, MAX_HZ)
        if (filtered.isEmpty()) return silentFrame()

        val logged = applyLogScale(filtered)
        val z = normalizeByZScore(logged)
        val normalized = normalizeMinMax(z) ?: return silentOrDecay() // flat noise → silence

        val bands = resampleToBands(normalized, BAND_COUNT)
        applyGain(bands, gain)
        return finishFrame(bands)
    }

    // --- Realistic: spectrum shape × same absolute dynamics ---

    private fun processRealistic(fft: ByteArray, captureSize: Int, sampleRateHz: Int): SpectrumFrame {
        if (!exoPlayer.isPlaying) return silentOrDecay()

        val gain = dynamicsGain()
        if (gain <= 0.001f) return silentOrDecay()

        val mags = fftMagnitudes(fft)
        if (mags.isEmpty()) return silentFrame()

        val filtered = filterFrequency(mags, sampleRateHz, captureSize, MIN_HZ, MAX_HZ)
        if (filtered.isEmpty()) return silentFrame()

        val logged = applyLogScale(filtered)
        var peak = 1e-3f
        for (v in logged) peak = max(peak, v)
        val shape = FloatArray(logged.size) { i -> (logged[i] / peak).coerceIn(0f, 1f) }
        val bands = resampleToBands(shape, BAND_COUNT)
        applyGain(bands, gain)
        return finishFrame(bands)
    }

    /**
     * Maps track PCM loudness → visual amplitude.
     * Quiet passages (relative to recent track peak) collapse to near-zero in both modes.
     */
    private fun dynamicsGain(): Float {
        val rel = trackLoudness()
        dynamicsEnv = if (rel >= dynamicsEnv) {
            dynamicsEnv * 0.3f + rel * 0.7f
        } else {
            dynamicsEnv * 0.65f + rel * 0.35f
        }
        if (dynamicsEnv < QUIET_FLOOR) return 0f
        val x = ((dynamicsEnv - QUIET_FLOOR) / (1f - QUIET_FLOOR)).coerceIn(0f, 1f)
        // Expand quiet→loud range so mid-quiet sections stay visually subdued.
        return x.toDouble().pow(1.65).toFloat()
    }

    private fun applyGain(bands: FloatArray, gain: Float) {
        for (i in bands.indices) {
            bands[i] = (bands[i] * gain).coerceIn(0f, 1f)
        }
    }

    private fun trackLoudness(): Float {
        val tee = trackLevelMeter.rms
        if (tee > TEE_GATE) {
            trackTeePeak = when {
                trackTeePeak < TEE_GATE -> tee
                tee > trackTeePeak -> tee
                else -> max(tee, trackTeePeak * TRACK_PEAK_DECAY)
            }
            return (tee / trackTeePeak.coerceAtLeast(TEE_GATE)).coerceIn(0f, 1f)
        }
        // Device muted → cannot fall back to Visualizer waveform.
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) return 0f
        if (waveformRms < 0.02f) return 0f
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val volFrac = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol)
            .coerceIn(0.08f, 1f)
        val compensated = waveformRms / volFrac
        if (compensated < 0.04f) return 0f
        return (compensated / 0.3f).coerceIn(0f, 1f)
    }

    private fun silentOrDecay(): SpectrumFrame {
        silenceFrames++
        beatPulse *= 0.5f
        dynamicsEnv *= 0.55f
        return if (silenceFrames >= 2) silentFrame() else decayTowardSilence(_frames.value)
    }

    private fun finishFrame(bands: FloatArray): SpectrumFrame {
        silenceFrames = 0
        var energySum = 0f
        for (v in bands) energySum += v
        val rms = (energySum / bands.size).coerceIn(0f, 1f)
        if (rms < 0.02f) return silentOrDecay()
        val onset = (rms - prevRms).coerceAtLeast(0f)
        prevRms = rms
        beatPulse = max(beatPulse * 0.75f, onset * 4f).coerceIn(0f, 1f)
        return SpectrumFrame(bands, rms, beatPulse, System.currentTimeMillis())
    }

    private fun decayTowardSilence(previous: SpectrumFrame?): SpectrumFrame {
        val prev = previous ?: return silentFrame()
        val bands = FloatArray(prev.bands.size) { i -> prev.bands[i] * 0.5f }
        var sum = 0f
        for (v in bands) sum += v
        if (sum < 0.04f) return silentFrame()
        return SpectrumFrame(bands, sum / bands.size, beatPulse * 0.5f, System.currentTimeMillis())
    }

    private fun silentFrame(): SpectrumFrame =
        SpectrumFrame(FloatArray(BAND_COUNT), 0f, 0f, System.currentTimeMillis())

    private fun releaseVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {
        }
        visualizer = null
    }

    private companion object {
        const val TAG = "AudioSpectrum"
        const val BAND_COUNT = 48
        const val MIN_HZ = 50
        const val MAX_HZ = 14_000
        const val TEE_GATE = 0.001f
        /** Relative to recent track peak; below this bars collapse (quiet passage). */
        const val QUIET_FLOOR = 0.16f
        const val TRACK_PEAK_DECAY = 0.9985f
        /** Slack for ExoPlayer AudioTrack buffer after Visualizer/Tee capture. */
        const val EXOPLAYER_BUFFER_COMPENSATION_MS = 80L
        const val DEFAULT_OUTPUT_DELAY_MS = 120L
        const val MIN_OUTPUT_DELAY_MS = 50L
        const val MAX_OUTPUT_DELAY_MS = 220L
        const val MAX_PENDING_FRAMES = 24

        fun fftMagnitudes(fft: ByteArray): FloatArray {
            // Skip DC/Nyquist pair (first 2 bytes), then re/im pairs — same as ComposeCircle.
            val data = fft.copyOfRange(2, fft.size)
            val size = data.size / 2
            if (size <= 0) return FloatArray(0)
            return FloatArray(size) { i ->
                val re = data[2 * i].toInt().toDouble()
                val im = data.getOrElse(2 * i + 1) { 0 }.toInt().toDouble()
                hypot(re, im).toFloat()
            }
        }

        fun filterFrequency(
            mags: FloatArray,
            sampleRateHz: Int,
            captureSize: Int,
            minFreq: Int,
            maxFreq: Int,
        ): FloatArray {
            if (mags.isEmpty()) return mags
            val resolution = (sampleRateHz / 2.0) / (captureSize / 2 - 1).coerceAtLeast(1)
            val start = (minFreq / resolution).toInt().coerceIn(0, mags.lastIndex)
            val end = (maxFreq / resolution).toInt().coerceIn(start, mags.lastIndex)
            return mags.copyOfRange(start, end + 1)
        }

        fun applyLogScale(data: FloatArray): FloatArray {
            val minValue = data.minOrNull() ?: 0f
            val offset = if (minValue < 1f) 1f - minValue else 0f
            val epsilon = 1e-6f
            return FloatArray(data.size) { i ->
                val shifted = data[i] + offset + epsilon
                val safe = if (shifted <= 0f || shifted.isNaN()) epsilon else shifted
                log10(safe.toDouble()).toFloat()
            }
        }

        fun normalizeByZScore(data: FloatArray): FloatArray {
            if (data.isEmpty()) return data
            val mean = data.average().toFloat()
            val variance = data.map { (it - mean).toDouble().pow(2.0) }.average()
            val std = sqrt(variance).toFloat()
            if (std == 0f || std.isNaN()) return FloatArray(data.size)
            return FloatArray(data.size) { i -> (data[i] - mean) / std }
        }

        /** ComposeCircle: if range too small → silence (don't inflate noise). */
        fun normalizeMinMax(data: FloatArray): FloatArray? {
            val max = data.maxOrNull() ?: return null
            val min = data.minOrNull() ?: return null
            if (max - min <= 1f) return null
            return FloatArray(data.size) { i -> ((data[i] - min) / (max - min)).coerceIn(0f, 1f) }
        }

        fun resampleToBands(src: FloatArray, bandCount: Int): FloatArray {
            if (src.isEmpty()) return FloatArray(bandCount)
            if (src.size == bandCount) return src.copyOf()
            val out = FloatArray(bandCount)
            for (b in 0 until bandCount) {
                val t0 = b / bandCount.toFloat()
                val t1 = (b + 1) / bandCount.toFloat()
                // Log-ish distribution across filtered bins.
                val i0 = (exp(ln(src.size.toDouble()) * t0) - 1).toInt().coerceIn(0, src.lastIndex)
                val i1 = (exp(ln(src.size.toDouble()) * t1) - 1).toInt().coerceIn(i0, src.lastIndex)
                var sum = 0f
                var peak = 0f
                for (i in i0..i1) {
                    sum += src[i]
                    peak = max(peak, src[i])
                }
                out[b] = 0.35f * (sum / (i1 - i0 + 1)) + 0.65f * peak
            }
            return out
        }
    }
}
