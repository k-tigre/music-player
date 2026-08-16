package by.tigre.media.platform.playback.eq

/**
 * Speech-oriented 6-band UI EQ for AudioBook (C2): denser around formants,
 * top shelf at 12 kHz so the last slider does not also yank mid-sibilance at 4 kHz.
 */
internal object AudiobookSpeechEq {
    val bandCentersHz: FloatArray = floatArrayOf(
        160f, 400f, 800f, 1600f, 4000f, 12000f,
    )

    val presets: List<EqPresetDefinition> = listOf(
        EqPresetDefinition("flat", "Flat", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f)),
        EqPresetDefinition("voice", "Voice", floatArrayOf(-1f, -2f, 0f, 2f, 3f, 1f)),
        EqPresetDefinition("warm", "Warm", floatArrayOf(1f, 2f, 1.5f, 0f, -1f, -2f)),
        EqPresetDefinition("bright", "Bright", floatArrayOf(-2f, -1f, 0f, 1f, 3f, 2.5f)),
        EqPresetDefinition("night", "Night", floatArrayOf(0f, 1f, 0f, -1f, -2.5f, -3f)),
    )
}
