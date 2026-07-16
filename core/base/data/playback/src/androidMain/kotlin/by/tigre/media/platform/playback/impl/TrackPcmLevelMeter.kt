package by.tigre.media.platform.playback.impl

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import by.tigre.logger.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * PCM loudness meter tapped before AudioTrack / device volume.
 * Reflects track dynamics (quiet ending → low level), not system volume.
 */
@OptIn(UnstableApi::class)
internal class TrackPcmLevelMeter : TeeAudioProcessor.AudioBufferSink {

    @Volatile
    var rms: Float = 0f
        private set

    private var envelope = 0f
    private var sampleRateHz = 44_100
    private var channelCount = 2
    private var encoding = C.ENCODING_PCM_16BIT

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.sampleRateHz = sampleRateHz
        this.channelCount = channelCount.coerceAtLeast(1)
        this.encoding = encoding
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        if (!buffer.hasRemaining()) return
        val view = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        val instant = when (encoding) {
            C.ENCODING_PCM_FLOAT -> rmsFloat(view)
            C.ENCODING_PCM_24BIT, C.ENCODING_PCM_32BIT -> rms32ish(view)
            else -> rms16(view)
        }
        if (instant > 0.0001f && rms < 0.0001f) {
            Log.d("TrackPcmLevel") { "first PCM rms instant=$instant encoding=$encoding" }
        }
        // Fast attack / faster release so quiet passages drop quickly for the visualizer.
        envelope = if (instant >= envelope) {
            envelope * 0.2f + instant * 0.8f
        } else {
            envelope * 0.72f + instant * 0.28f
        }
        rms = envelope.coerceIn(0f, 1f)
    }

    private fun rms16(buf: ByteBuffer): Float {
        var sum = 0.0
        var n = 0
        while (buf.remaining() >= 2) {
            val s = buf.short / 32768.0
            sum += s * s
            n++
        }
        return if (n == 0) 0f else sqrt(sum / n).toFloat()
    }

    private fun rmsFloat(buf: ByteBuffer): Float {
        var sum = 0.0
        var n = 0
        while (buf.remaining() >= 4) {
            val s = buf.float.toDouble()
            sum += s * s
            n++
        }
        return if (n == 0) 0f else sqrt(sum / n).toFloat()
    }

    private fun rms32ish(buf: ByteBuffer): Float {
        // Approximate with 32-bit int samples when available.
        var sum = 0.0
        var n = 0
        while (buf.remaining() >= 4) {
            val s = buf.int / 2147483648.0
            sum += s * s
            n++
        }
        return if (n == 0) 0f else sqrt(sum / n).toFloat()
    }

    fun reset() {
        envelope = 0f
        rms = 0f
    }
}
