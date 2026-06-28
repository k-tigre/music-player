package by.tigre.audiobook.nighttimer

import kotlinx.coroutines.flow.StateFlow

interface NightTimerShakeDebug {
    val shakeConfig: StateFlow<NightTimerShakeConfig>
    val shakeConfigSource: StateFlow<NightTimerShakeConfigSource>
    val shakeConfigFetching: StateFlow<Boolean>
    val shakeDebugState: StateFlow<NightTimerShakeDebugState>

    fun updateShakeConfig(config: NightTimerShakeConfig)
    fun resetShakeConfigToDefaults()
    fun refreshShakeConfig()
    fun setShakeTestMode(enabled: Boolean)
    fun resetShakeDetection()
}
