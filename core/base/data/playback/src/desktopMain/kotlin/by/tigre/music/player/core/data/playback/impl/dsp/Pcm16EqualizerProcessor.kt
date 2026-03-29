package by.tigre.music.player.core.data.playback.impl.dsp

/**
 * In-place peaking EQ for interleaved 16-bit PCM ([DesktopEqualizerPresets]).
 * Thread-safe: [setPreset] and [processInterleavedPcmS16] share one lock.
 */
internal class Pcm16EqualizerProcessor(
    private val channels: Int,
    private val sampleRate: Float,
    gainsDb: FloatArray,
    centersHz: FloatArray,
) {
    private val chCount = channels.coerceAtLeast(1)

    private val lock = Any()

    @Volatile
    private var filtersPerChannel: Array<Array<BiQuad>> =
        buildFilters(gainsDb, centersHz)

    private fun buildFilters(gains: FloatArray, centers: FloatArray): Array<Array<BiQuad>> =
        Array(chCount) {
            Array(centers.size) { idx ->
                BiQuad().apply {
                    setPeakingDb(
                        fc = centers[idx].toDouble(),
                        sampleRate = sampleRate.toDouble(),
                        q = 1.4142135623730951,
                        dbGain = gains[idx].toDouble(),
                    )
                }
            }
        }

    fun setPreset(presetIndex: Int) {
        val gains = DesktopEqualizerPresets.gainsForPreset(presetIndex)
        val centers = DesktopEqualizerPresets.bandCentersHz
        synchronized(lock) {
            filtersPerChannel = buildFilters(gains, centers)
        }
    }

    fun setGains(gainsDb: FloatArray) {
        val centers = DesktopEqualizerPresets.bandCentersHz
        if (gainsDb.size != centers.size) return
        synchronized(lock) {
            filtersPerChannel = buildFilters(gainsDb.copyOf(), centers)
        }
    }

    fun processInterleavedPcmS16(b: ByteArray, off: Int, len: Int, bigEndian: Boolean) {
        if (len <= 0) return
        synchronized(lock) {
            processPcm16InterleavedUnsynced(b, off, len, bigEndian, filtersPerChannel)
        }
    }

    private fun processPcm16InterleavedUnsynced(
        b: ByteArray,
        off: Int,
        len: Int,
        bigEndian: Boolean,
        filters: Array<Array<BiQuad>>,
    ) {
        var i = off
        val end = off + len
        while (i + 1 < end) {
            val lo = b[i].toInt() and 0xff
            val hi = b[i + 1].toInt() and 0xff
            var sample = if (bigEndian) (lo shl 8) or hi else (hi shl 8) or lo
            if (sample and 0x8000 != 0) sample -= 0x10000
            var x = sample / 32768.0
            val ch = ((i - off) ushr 1) % chCount
            for (f in filters[ch]) {
                x = f.process(x)
            }
            var out = (x * 32768.0).toInt().coerceIn(-32768, 32767)
            if (bigEndian) {
                b[i] = ((out shr 8) and 0xff).toByte()
                b[i + 1] = (out and 0xff).toByte()
            } else {
                b[i] = (out and 0xff).toByte()
                b[i + 1] = ((out shr 8) and 0xff).toByte()
            }
            i += 2
        }
    }

    companion object {
        fun forPreset(channels: Int, sampleRate: Float, presetIndex: Int): Pcm16EqualizerProcessor {
            val gains = DesktopEqualizerPresets.gainsForPreset(presetIndex)
            return Pcm16EqualizerProcessor(
                channels,
                sampleRate,
                gains,
                DesktopEqualizerPresets.bandCentersHz,
            )
        }

        fun forGains(
            channels: Int,
            sampleRate: Float,
            gainsDb: FloatArray,
            centersHz: FloatArray,
        ): Pcm16EqualizerProcessor =
            Pcm16EqualizerProcessor(channels, sampleRate, gainsDb, centersHz)
    }
}
