package by.tigre.media.platform.playback

import kotlin.math.roundToInt

object PlaybackSpeed {
    const val MIN = 0.5f
    const val MAX = 2.0f
    const val STEP = 0.1f
    const val DEFAULT = 1.0f

    val sliderSteps: Int
        get() = stepCount - 2

    private val stepCount: Int
        get() = ((MAX - MIN) / STEP).roundToInt() + 1

    fun coerce(value: Float): Float = quantize(value.coerceIn(MIN, MAX))

    fun quantize(value: Float): Float {
        val steps = ((value - MIN) / STEP).roundToInt()
        return (MIN + steps * STEP).coerceIn(MIN, MAX)
    }

    fun format(speed: Float): String {
        val quantized = quantize(speed)
        val numeric = if (quantized % 1f == 0f) {
            quantized.toInt().toString()
        } else {
            ((quantized * 10).roundToInt() / 10f).toString()
        }
        return "${numeric}×"
    }
}
