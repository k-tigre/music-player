package by.tigre.music.player.core.data.playback.impl.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

internal class BiQuad {

    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun setPeakingDb(fc: Double, sampleRate: Double, q: Double, dbGain: Double) {
        val a = 10.0.pow(dbGain / 40.0)
        val w0 = 2 * PI * fc / sampleRate
        val cosw0 = cos(w0)
        val sinw0 = sin(w0)
        val alpha = sinw0 / (2 * q)
        val b0n = 1 + alpha * a
        val b1n = -2 * cosw0
        val b2n = 1 - alpha * a
        val a0n = 1 + alpha / a
        val a1n = -2 * cosw0
        val a2n = 1 - alpha / a
        b0 = b0n / a0n
        b1 = b1n / a0n
        b2 = b2n / a0n
        a1 = a1n / a0n
        a2 = a2n / a0n
        x1 = 0.0
        x2 = 0.0
        y1 = 0.0
        y2 = 0.0
    }

    fun process(x: Double): Double {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = x
        y2 = y1
        y1 = y
        return y
    }
}
