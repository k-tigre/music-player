package by.tigre.audiobook.nighttimer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.os.SystemClock
import by.tigre.logger.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * Extends the night timer after two distinct shakes in a row.
 * Flipping the phone over and back naturally produces two shakes — no angle tracking required.
 *
 * Sensor listening is only active while detection is enabled (last minute of the timer).
 * A partial wake lock is held for that window so accelerometer events are delivered with the
 * screen off / Activity destroyed (non-wake-up sensors otherwise lose events while the AP sleeps).
 */
internal class NightTimerShakeExtender(
    context: Context,
    private val configProvider: () -> NightTimerShakeConfig,
    private val onExtend: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = resolveAccelerometer(sensorManager)
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val wakeLock: PowerManager.WakeLock =
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
        }

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
        ensureSensorRegistered()
        acquireWakeLock()
        publishDebugState()
    }

    fun disable() {
        detectionEnabled = false
        if (!testMode) {
            stopSensorIfIdle()
            releaseWakeLock()
        }
        publishDebugState()
    }

    fun setTestMode(enabled: Boolean) {
        testMode = enabled
        if (enabled) {
            ensureSensorRegistered()
            acquireWakeLock()
        } else {
            stopSensorIfIdle()
            if (!detectionEnabled) {
                releaseWakeLock()
            }
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

    /** Called when the night timer starts; sensor stays off until [enable]. */
    fun start() {
        resetDetectionState()
    }

    fun stop() {
        detectionEnabled = false
        testMode = false
        if (sensorRegistered) {
            sensorManager.unregisterListener(listener)
            sensorRegistered = false
        }
        releaseWakeLock()
        resetDetectionState()
        _debugState.value = NightTimerShakeDebugState()
    }

    private fun ensureSensorRegistered() {
        if (sensorRegistered) return
        val sensor = accelerometer
        if (sensor == null) {
            Log.w(TAG) { "No accelerometer available" }
            return
        }
        sensorRegistered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        if (!sensorRegistered) {
            sensorRegistered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        Log.d(TAG) {
            "Sensor registered=$sensorRegistered wakeUp=${sensor.isWakeUpSensor}"
        }
    }

    private fun stopSensorIfIdle() {
        if (!detectionEnabled && !testMode && sensorRegistered) {
            sensorManager.unregisterListener(listener)
            sensorRegistered = false
            _debugState.value = _debugState.value.copy(sensorActive = false)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock.isHeld) return
        // Safety timeout: shake gate is < 60s; allow a little slack if the tick is delayed.
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
        Log.d(TAG) { "WakeLock acquired" }
    }

    private fun releaseWakeLock() {
        if (!wakeLock.isHeld) return
        wakeLock.release()
        Log.d(TAG) { "WakeLock released" }
    }

    private companion object {
        const val TAG = "NightTimerShake"
        const val WAKE_LOCK_TAG = "audiobook:NightTimerShake"
        const val WAKE_LOCK_TIMEOUT_MS = 90_000L

        fun resolveAccelerometer(sensorManager: SensorManager): Sensor? {
            // Prefer wake-up variant so events can wake the AP when the screen is off.
            return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, /* wakeUp */ true)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
    }
}
