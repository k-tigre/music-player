package by.tigre.audiobook.nighttimer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Detects screen-facing-down using Z-axis hysteresis so borderline readings don’t fire twice.
 */
internal class FaceDownFlipExtender(
    context: Context,
    private val onFaceDown: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /** True while device is considered “face down” until Z clears the exit threshold. */
    private var faceDownLatched = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
            val z = event.values[2]
            if (!faceDownLatched) {
                if (z < FACE_DOWN_ENTER_Z) {
                    faceDownLatched = true
                    onFaceDown()
                }
            } else if (z > FACE_DOWN_EXIT_Z) {
                faceDownLatched = false
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    /** Call when the “last minute” window starts so a prior latched pose doesn’t block the next flip. */
    fun resetDetectionState() {
        faceDownLatched = false
    }

    fun start() {
        val sensor = accelerometer ?: return
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
        faceDownLatched = false
    }

    private companion object {
        /** m/s²; must dip below this to count as face-down (enter). */
        const val FACE_DOWN_ENTER_Z = -7.5f

        /** m/s²; must rise above this to leave face-down (exit), avoids chatter at the boundary. */
        const val FACE_DOWN_EXIT_Z = -4f
    }
}
