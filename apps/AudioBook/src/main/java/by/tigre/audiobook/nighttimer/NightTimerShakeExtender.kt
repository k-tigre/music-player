package by.tigre.audiobook.nighttimer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * Extends the night timer after two distinct shakes in a row.
 * Flipping the phone over and back naturally produces two shakes — no angle tracking required.
 */
internal class NightTimerShakeExtender(
    context: Context,
    private val configProvider: () -> NightTimerShakeConfig,
    private val onExtend: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var detectionEnabled = false
    private var testMode = false
    private var shakeCount = 0
    private var lastShakeAtMs = 0L
    private var firstShakeAtMs = 0L
    private var completedPairs = 0
    private var sensorRegistered = false

    private val _debugState = MutableStateFlow(NightTimerShakeDebugState())
    val debugState: StateFlow<NightTimerShakeDebugState> = _debugState.asStateFlow()

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
            if (!detectionEnabled && !testMode) return

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gForce = currentGForce(x, y, z)
            val config = configProvider()

            publishDebugState(gForce = gForce)

            if (!isShake(gForce, config)) return

            val now = SystemClock.elapsedRealtime()
            if (now - lastShakeAtMs < config.debounceMs) return
            lastShakeAtMs = now

            if (shakeCount == 0) {
                shakeCount = 1
                firstShakeAtMs = now
                publishDebugState(gForce = gForce, firstShakePassed = true)
                return
            }

            if (now - firstShakeAtMs > config.pairMaxGapMs) {
                shakeCount = 1
                firstShakeAtMs = now
                publishDebugState(gForce = gForce, firstShakePassed = true, pairPassed = false)
                return
            }

            shakeCount = 0
            firstShakeAtMs = 0L
            completedPairs++
            publishDebugState(
                gForce = gForce,
                firstShakePassed = true,
                pairPassed = true,
                completedPairs = completedPairs,
            )

            if (testMode) {
                resetDetectionState(keepCompletedPairs = true)
            } else {
                onExtend()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun currentGForce(x: Float, y: Float, z: Float): Float {
        return sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
    }

    private fun isShake(gForce: Float, config: NightTimerShakeConfig): Boolean {
        return gForce >= config.gForceThreshold
    }

    private fun publishDebugState(
        gForce: Float = _debugState.value.currentGForce,
        firstShakePassed: Boolean = shakeCount > 0,
        pairPassed: Boolean = false,
        completedPairs: Int = _debugState.value.completedPairs,
    ) {
        _debugState.value = NightTimerShakeDebugState(
            sensorActive = detectionEnabled || testMode,
            detectionEnabled = detectionEnabled,
            testMode = testMode,
            currentGForce = gForce,
            shakeCount = shakeCount,
            firstShakePassed = firstShakePassed,
            pairPassed = pairPassed,
            completedPairs = completedPairs,
        )
    }

    fun enable() {
        detectionEnabled = true
        publishDebugState()
    }

    fun disable() {
        detectionEnabled = false
        if (!testMode) {
            stopSensorIfIdle()
        }
        publishDebugState()
    }

    fun setTestMode(enabled: Boolean) {
        testMode = enabled
        if (enabled) {
            ensureSensorRegistered()
        } else {
            stopSensorIfIdle()
        }
        if (!enabled) {
            resetDetectionState(keepCompletedPairs = false)
        } else {
            publishDebugState()
        }
    }

    fun resetDetectionState(keepCompletedPairs: Boolean = false) {
        shakeCount = 0
        lastShakeAtMs = 0L
        firstShakeAtMs = 0L
        if (!keepCompletedPairs) {
            completedPairs = 0
        }
        publishDebugState(completedPairs = completedPairs)
    }

    fun start() {
        ensureSensorRegistered()
    }

    fun stop() {
        if (sensorRegistered) {
            sensorManager.unregisterListener(listener)
            sensorRegistered = false
        }
        detectionEnabled = false
        testMode = false
        resetDetectionState()
        _debugState.value = NightTimerShakeDebugState()
    }

    private fun ensureSensorRegistered() {
        if (sensorRegistered) return
        val sensor = accelerometer ?: return
        sensorRegistered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        if (!sensorRegistered) {
            sensorRegistered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopSensorIfIdle() {
        if (!detectionEnabled && !testMode && sensorRegistered) {
            sensorManager.unregisterListener(listener)
            sensorRegistered = false
            _debugState.value = _debugState.value.copy(sensorActive = false)
        }
    }
}
