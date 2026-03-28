package by.tigre.music.player.core.data.playback.impl.dsp

internal object DesktopEqualizerPresets {

    const val GAIN_DB_MIN = -12f
    const val GAIN_DB_MAX = 12f

    val bandCentersHz = floatArrayOf(62f, 250f, 1000f, 4000f, 10000f)

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

    private val gainsDb: Array<FloatArray> = arrayOf(
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

    fun gainsForPreset(index: Int): FloatArray {
        val i = index.coerceIn(0, gainsDb.lastIndex)
        return gainsDb[i].copyOf()
    }

    fun allBuiltInBandGainsDb(): List<List<Float>> = gainsDb.map { row -> row.map { it } }
}
