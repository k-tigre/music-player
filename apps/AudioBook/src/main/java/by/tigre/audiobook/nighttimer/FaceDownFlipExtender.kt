package by.tigre.audiobook.nighttimer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Detects two ~180° flips in a row to extend the night timer:
 * resting pose → flipped (~180°) → flipped back (~180°).
 *
 * Uses the gravity vector from the accelerometer, not fixed "screen up/down" poses.
 */
internal class FaceDownFlipExtender(
    context: Context,
    private val onFlipExtend: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private enum class Phase {
        /** Waiting for a stable resting orientation to capture as baseline. */
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

    private var previousSample: OrientationVector? = null
    private var stableSampleCount = 0
    private var lastStableOrientation: OrientationVector? = null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (!detectionEnabled) return
            if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt(x * x + y * y + z * z)
            if (magnitude !in GRAVITY_MIN..GRAVITY_MAX) {
                resetStabilityTracking()
                return
            }

            val current = OrientationVector(x, y, z).normalized() ?: run {
                resetStabilityTracking()
                return
            }

            val previous = previousSample
            if (previous != null) {
                if (previous.angleTo(current) <= STABLE_ANGLE_THRESHOLD_DEG) {
                    stableSampleCount++
                } else {
                    stableSampleCount = 1
                }
            } else {
                stableSampleCount = 1
            }
            previousSample = current

            if (stableSampleCount < STABLE_SAMPLES_REQUIRED) return

            lastStableOrientation = current
            handleStableOrientation(current)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun handleStableOrientation(orientation: OrientationVector) {
        when (phase) {
            Phase.CapturingRest -> {
                baselineOrientation = orientation
                phase = Phase.AwaitingFirstFlip
            }
            Phase.AwaitingFirstFlip -> {
                val baseline = baselineOrientation ?: return
                if (baseline.angleTo(orientation) >= MIN_FLIP_ANGLE_DEG) {
                    flippedOrientation = orientation
                    phase = Phase.AwaitingSecondFlip
                    resetStabilityTracking()
                }
            }
            Phase.AwaitingSecondFlip -> {
                val flipped = flippedOrientation ?: return
                if (flipped.angleTo(orientation) >= MIN_FLIP_ANGLE_DEG) {
                    onFlipExtend()
                    baselineOrientation = orientation
                    flippedOrientation = null
                    phase = Phase.AwaitingFirstFlip
                    resetStabilityTracking()
                }
            }
        }
    }

    private fun resetStabilityTracking() {
        previousSample = null
        stableSampleCount = 0
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
     * If the phone is already resting, capture that pose and wait for the first flip.
     */
    fun resetDetectionState() {
        resetStabilityTracking()
        val stable = lastStableOrientation
        if (stable != null) {
            baselineOrientation = stable
            flippedOrientation = null
            phase = Phase.AwaitingFirstFlip
        } else {
            baselineOrientation = null
            flippedOrientation = null
            phase = Phase.CapturingRest
        }
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
        resetStabilityTracking()
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
    }

    private companion object {
        /** Minimum angle between resting poses to count as a flip (~180°, 150° also accepted). */
        const val MIN_FLIP_ANGLE_DEG = 150f

        /** Consecutive samples within this angle are treated as "device at rest". */
        const val STABLE_ANGLE_THRESHOLD_DEG = 12f

        const val STABLE_SAMPLES_REQUIRED = 5

        /** m/s²; ignore readings while the device is moving or shaken. */
        const val GRAVITY_MIN = 8f
        const val GRAVITY_MAX = 11.5f
    }
}
