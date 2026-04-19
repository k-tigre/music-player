package by.tigre.music.player.core.data.playback.impl.dsp

import kotlin.math.ln
internal object DesktopEqualizerPresets {

    const val GAIN_DB_MIN = -12f
    const val GAIN_DB_MAX = 12f

    /**
     * Graphic EQ band centers (Hz): sub ~32 Hz, ISO-style spacing through highs, **16 kHz** and **20 kHz** air bands.
     * (Common references: third-octave ISO ~31.5 Hz … 16 kHz; consumer EQs often add 18–20 kHz as top shelf.)
     */
    val bandCentersHz = floatArrayOf(
        32f, 64f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f, 20000f,
    )

    val names: List<String> = listOf(
        "Normal",
        "Classical",
        "Dance",
        "Flat",
        "Folk",
        "Heavy Metal",
        "Hip Hop",
        "Jazz",
        "Pop",
        "Rock",
    )

    /** Legacy five-band anchors used to derive [gainsDb] for [bandCentersHz]. */
    private val legacyCentersHz = floatArrayOf(62f, 250f, 1000f, 4000f, 10000f)

    private val gainsDbLegacy: Array<FloatArray> = arrayOf(
        floatArrayOf(0f, 0f, 0f, 0f, 0f),
        floatArrayOf(3f, 0f, -2f, -2f, 2f),
        floatArrayOf(4f, 2f, -1f, 0f, 3f),
        floatArrayOf(0f, 0f, 0f, 0f, 0f),
        floatArrayOf(1f, 2f, 2f, 0f, -1f),
        floatArrayOf(4f, 2f, -2f, 1f, 4f),
        floatArrayOf(5f, 3f, -1f, -2f, 2f),
        floatArrayOf(2f, 2f, 0f, 1f, 3f),
        floatArrayOf(-1f, 2f, 3f, 2f, 1f),
        floatArrayOf(3f, 1f, -2f, -1f, 3f),
    )

    private val gainsDb: Array<FloatArray> =
        gainsDbLegacy.map { expandLegacyFiveBandToCurrentBands(it) }.toTypedArray()

    fun gainsForPreset(index: Int): FloatArray {
        val i = index.coerceIn(0, gainsDb.lastIndex)
        return gainsDb[i].copyOf()
    }

    fun allBuiltInBandGainsDb(): List<List<Float>> = gainsDb.map { row -> row.map { it } }

    private fun expandLegacyFiveBandToCurrentBands(legacyGains: FloatArray): FloatArray {
        require(legacyGains.size == legacyCentersHz.size)
        return FloatArray(bandCentersHz.size) { i ->
            interpolateDb(bandCentersHz[i].toDouble(), legacyCentersHz, legacyGains)
        }
    }

    private fun interpolateDb(hz: Double, oldC: FloatArray, oldG: FloatArray): Float {
        val log = ln(hz)
        val logs = oldC.map { ln(it.toDouble()) }
        if (log <= logs[0]) return oldG[0]
        if (log >= logs.last()) return oldG.last()
        for (i in 0 until logs.lastIndex) {
            if (log <= logs[i + 1]) {
                val t = ((log - logs[i]) / (logs[i + 1] - logs[i])).toFloat()
                return oldG[i] + t * (oldG[i + 1] - oldG[i])
            }
        }
        return oldG.last()
    }
}
