package by.tigre.media.platform.playback.eq

import kotlin.math.ln

internal object EqGainInterpolation {
    fun interpolateGainDb(hz: Double, centersHz: FloatArray, gainsDb: FloatArray): Float {
        require(centersHz.size == gainsDb.size)
        require(centersHz.isNotEmpty())
        val log = ln(hz.coerceAtLeast(1.0))
        val logs = DoubleArray(centersHz.size) { ln(centersHz[it].toDouble().coerceAtLeast(1.0)) }
        if (log <= logs[0]) return gainsDb[0]
        if (log >= logs[logs.lastIndex]) return gainsDb[gainsDb.lastIndex]
        for (i in 0 until logs.lastIndex) {
            if (log <= logs[i + 1]) {
                val t = ((log - logs[i]) / (logs[i + 1] - logs[i])).toFloat()
                return gainsDb[i] + t * (gainsDb[i + 1] - gainsDb[i])
            }
        }
        return gainsDb[gainsDb.lastIndex]
    }

    fun remapGainsToCenters(
        gainsDb: List<Float>,
        fromCentersHz: FloatArray,
        toCentersHz: FloatArray,
    ): List<Float> {
        require(fromCentersHz.size == gainsDb.size)
        val fromG = gainsDb.toFloatArray()
        return List(toCentersHz.size) { i ->
            interpolateGainDb(toCentersHz[i].toDouble(), fromCentersHz, fromG)
        }
    }

    /**
     * Align saved gains to [toCentersHz]. Same length → as-is; length matches music UI →
     * remap from music centers; otherwise pad/truncate by index.
     */
    fun alignOrRemapToUiBands(
        saved: List<Float>?,
        toCentersHz: FloatArray,
        legacyMusicCentersHz: FloatArray = UiEqConfig.MusicBandCentersHz,
    ): List<Float> {
        if (saved.isNullOrEmpty()) return List(toCentersHz.size) { 0f }
        if (saved.size == toCentersHz.size) return saved
        if (saved.size == legacyMusicCentersHz.size) {
            return remapGainsToCenters(saved, legacyMusicCentersHz, toCentersHz)
        }
        return List(toCentersHz.size) { i -> saved.getOrNull(i) ?: 0f }
    }
}
