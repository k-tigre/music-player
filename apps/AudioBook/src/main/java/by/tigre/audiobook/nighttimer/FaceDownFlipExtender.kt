package by.tigre.audiobook.nighttimer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import by.tigre.audiobook.nighttimer.FaceDownFlipExtender.Companion.REST_LYING_DURATION_MS
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Extends the night timer after two ~180° flips:
 * 1. Lay the phone flat and hold still (~5 s) — baseline is captured.
 * 2. Flip ~180°, flip ~180° back — timer extends (no need to hold after each flip).
 * 3. After extend, hold still lying flat for ~5 s again before the next cycle.
 * 4. While waiting for flips, 5 s upright/in hand resets the cycle
 *    (lying on a nightstand does not count).
 *
 * Upright/hand poses are ignored until the phone is lying on a surface.
 */
internal class FaceDownFlipExtender(
    context: Context,
    private val onFlipExtend: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private enum class Phase {
        /** Waiting for the phone to lie flat and stay still. */
        CapturingRest,

        /** Baseline captured; waiting for the first ~180° flip. */
        AwaitingFirstFlip,

        /** First flip captured; waiting for a second ~180° flip back. */
        AwaitingSecondFlip,
    }

    private var phase = Phase.CapturingRest
    private var detectionEnabled = false

    private var baselineOrientation: OrientationVector? = null
    private var flippedOrientation: OrientationVector? = null

    private var lastStableOrientation: OrientationVector? = null

    /** When the phone started lying flat and still; null until conditions are met. */
    private var idleLyingStartedAtMs: Long? = null
    private var idlePreviousSample: OrientationVector? = null
    private var lastIdleProgressLogSecond = -1

    private var invalidMagnitudeLogCounter = 0

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (!detectionEnabled) return
            if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt(x * x + y * y + z * z)
            if (magnitude !in GRAVITY_MIN..GRAVITY_MAX) {
                invalidMagnitudeLogCounter++
                resetIdleLyingCapture()
                return
            }

            val current = OrientationVector(x, y, z).normalized() ?: run {
                resetIdleLyingCapture()
                return
            }

            when (phase) {
                Phase.CapturingRest -> {
                    if (updateIdleStill(current, requireLying = true)) {
                        captureIdleBaseline(current)
                    }
                }

                else -> {
                    if (updateIdleStill(current, requireLying = false)) {
                        resetAfterUprightHold()
                        return
                    }
                    handleFlipCapture(current)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    /**
     * Tracks how long the phone stays still in the expected pose.
     * @param requireLying true for initial arming (must lie flat); false while waiting for flips (must be upright/in hand).
     * @return true when [REST_LYING_DURATION_MS] has elapsed.
     */
    private fun updateIdleStill(current: OrientationVector, requireLying: Boolean): Boolean {
        val isLying = current.isLyingOnSurface()
        if (isLying != requireLying) {
            if (idleLyingStartedAtMs != null) {
                resetIdleLyingCapture()
            }
            return false
        }

        val previous = idlePreviousSample
        if (previous != null && previous.angleTo(current) > REST_ANGLE_THRESHOLD_DEG) {
            resetIdleLyingCapture()
            idlePreviousSample = current
            return false
        }
        idlePreviousSample = current

        val now = SystemClock.elapsedRealtime()
        val startedAt = idleLyingStartedAtMs
        if (startedAt == null) {
            idleLyingStartedAtMs = now
            lastIdleProgressLogSecond = -1
            return false
        }

        val elapsedSec = ((now - startedAt) / 1000L).toInt()
        if (elapsedSec != lastIdleProgressLogSecond && elapsedSec > 0) {
            lastIdleProgressLogSecond = elapsedSec
        }

        return now - startedAt >= REST_LYING_DURATION_MS
    }

    private fun captureIdleBaseline(orientation: OrientationVector) {
        lastStableOrientation = orientation
        baselineOrientation = orientation
        flippedOrientation = null
        phase = Phase.AwaitingFirstFlip
        resetIdleLyingCapture()
    }

    private fun resetAfterUprightHold() {
        resetIdleLyingCapture()
        baselineOrientation = null
        flippedOrientation = null
        phase = Phase.CapturingRest
    }

    /** Checks flip angle on every sensor sample — no post-flip hold required. */
    private fun handleFlipCapture(current: OrientationVector) {
        lastStableOrientation = current

        when (phase) {
            Phase.AwaitingFirstFlip -> {
                val baseline = baselineOrientation ?: return
                val angle = baseline.angleTo(current)
                if (angle < MIN_FLIP_ANGLE_DEG) return
                flippedOrientation = current
                phase = Phase.AwaitingSecondFlip
                resetIdleLyingCapture()
            }

            Phase.AwaitingSecondFlip -> {
                val flipped = flippedOrientation ?: return
                val angle = flipped.angleTo(current)
                if (angle < MIN_FLIP_ANGLE_DEG) return
                onFlipExtend()
            }

            Phase.CapturingRest -> Unit
        }
    }

    private fun resetIdleLyingCapture() {
        idleLyingStartedAtMs = null
        lastIdleProgressLogSecond = -1
        idlePreviousSample = null
    }

    /** Enables flip detection (last minute of the timer). */
    fun enable() {
        detectionEnabled = true
    }

    /** Disables flip detection outside the last minute. */
    fun disable() {
        detectionEnabled = false
    }

    /**
     * Call when the "last minute" window starts or after a successful extend.
     * Waits for the phone to lie flat and still for [REST_LYING_DURATION_MS].
     */
    fun resetDetectionState() {
        resetIdleLyingCapture()
        baselineOrientation = null
        flippedOrientation = null
        phase = Phase.CapturingRest
    }

    fun start() {
        val sensor = accelerometer ?: return
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
        detectionEnabled = false
        phase = Phase.CapturingRest
        baselineOrientation = null
        flippedOrientation = null
        lastStableOrientation = null
        invalidMagnitudeLogCounter = 0
        resetIdleLyingCapture()
    }

    private class OrientationVector(
        val x: Float,
        val y: Float,
        val z: Float,
    ) {
        fun normalized(): OrientationVector? {
            val magnitude = sqrt(x * x + y * y + z * z)
            if (magnitude < 1f) return null
            return OrientationVector(x / magnitude, y / magnitude, z / magnitude)
        }

        fun angleTo(other: OrientationVector): Float {
            val dot = (x * other.x + y * other.y + z * other.z).coerceIn(-1f, 1f)
            return Math.toDegrees(acos(dot.toDouble())).toFloat()
        }

        /** 0° = portrait upright in hand; ~90° = lying flat on a surface. */
        fun tiltFromPortraitUpDeg(): Float = angleTo(PORTRAIT_UP)

        /** Phone screen is roughly horizontal (face up or face down on a table). */
        fun isLyingOnSurface(): Boolean = tiltFromPortraitUpDeg() >= MIN_LYING_TILT_DEG
    }

    private companion object {
        val PORTRAIT_UP = OrientationVector(0f, 1f, 0f)

        /** Minimum angle between resting poses to count as a flip (~180°, 150° also accepted). */
        const val MIN_FLIP_ANGLE_DEG = 120f

        /** Max drift while lying still before the idle timer resets. */
        const val REST_ANGLE_THRESHOLD_DEG = 8f

        /** How long the phone must lie flat and still before baseline is captured. */
        const val REST_LYING_DURATION_MS = 5_000L

        /**
         * Minimum tilt from portrait-up to count as "lying on a surface".
         * Upright in hand ≈ 10–25°; flat on table ≈ 75–90°.
         */
        const val MIN_LYING_TILT_DEG = 55f

        /** m/s²; reject strong shake/flip acceleration, tolerate minor sensor drift at rest. */
        const val GRAVITY_MIN = 3f
        const val GRAVITY_MAX = 20.5f
    }
}
