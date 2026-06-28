package by.tigre.audiobook.debug.nighttimer

import by.tigre.audiobook.nighttimer.NightTimerShakeConfig
import by.tigre.audiobook.nighttimer.NightTimerShakeConfigSource
import by.tigre.audiobook.nighttimer.NightTimerShakeDebug
import by.tigre.audiobook.nighttimer.NightTimerShakeDebugState
import by.tigre.debug_settings.ComposableDebugPage
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.tools.platform.compose.ComposableView
import kotlinx.coroutines.flow.StateFlow

internal interface NightTimerShakeDebugComponent : ComposableDebugPage {

    val shakeConfig: StateFlow<NightTimerShakeConfig>
    val shakeConfigSource: StateFlow<NightTimerShakeConfigSource>
    val shakeConfigFetching: StateFlow<Boolean>
    val debugState: StateFlow<NightTimerShakeDebugState>

    fun updateGForceThreshold(value: Float)
    fun updateDebounceMs(value: Long)
    fun updatePairMaxGapMs(value: Long)
    fun resetConfigToDefaults()
    fun refreshConfig()
    fun setTestMode(enabled: Boolean)
    fun resetDetection()

    class Impl(
        componentContext: BaseComponentContext,
        private val shakeDebug: NightTimerShakeDebug,
    ) : NightTimerShakeDebugComponent, BaseComponentContext by componentContext {

        override val title: String = "Shake timer"
        override val view: ComposableView = NightTimerShakeDebugView(this)

        override val shakeConfig = shakeDebug.shakeConfig
        override val shakeConfigSource = shakeDebug.shakeConfigSource
        override val shakeConfigFetching = shakeDebug.shakeConfigFetching
        override val debugState = shakeDebug.shakeDebugState

        override fun updateGForceThreshold(value: Float) {
            shakeDebug.updateShakeConfig(shakeConfig.value.copy(gForceThreshold = value))
        }

        override fun updateDebounceMs(value: Long) {
            shakeDebug.updateShakeConfig(shakeConfig.value.copy(debounceMs = value))
        }

        override fun updatePairMaxGapMs(value: Long) {
            shakeDebug.updateShakeConfig(shakeConfig.value.copy(pairMaxGapMs = value))
        }

        override fun resetConfigToDefaults() {
            shakeDebug.resetShakeConfigToDefaults()
        }

        override fun refreshConfig() {
            shakeDebug.refreshShakeConfig()
        }

        override fun setTestMode(enabled: Boolean) {
            shakeDebug.setShakeTestMode(enabled)
        }

        override fun resetDetection() {
            shakeDebug.resetShakeDetection()
        }
    }
}
