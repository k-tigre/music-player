package by.tigre.media.platform.playback.eq

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidAudioRouteMonitor(
    context: Context,
) : AudioRouteMonitor {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val _currentRoute = MutableStateFlow(detectRoute())
    override val currentRoute: StateFlow<AudioRouteId> = _currentRoute.asStateFlow()

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            _currentRoute.value = detectRoute()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            _currentRoute.value = detectRoute()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(callback, null)
        _currentRoute.value = detectRoute()
    }

    fun release() {
        audioManager.unregisterAudioDeviceCallback(callback)
    }

    private fun detectRoute(): AudioRouteId {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val bt = devices.firstOrNull { it.isBluetoothOut() }
        if (bt != null) {
            val key = bt.stableBluetoothKey()
            return AudioRouteId(AudioRouteId.Kind.Bluetooth, key)
        }
        val wired = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
        if (wired != null) return AudioRouteId.WiredHeadset
        val speaker = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }
        if (speaker != null) return AudioRouteId.BuiltinSpeaker
        return AudioRouteId.Unknown
    }

    private fun AudioDeviceInfo.isBluetoothOut(): Boolean {
        if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) return true
        if (Build.VERSION.SDK_INT >= 31) {
            if (type == AudioDeviceInfo.TYPE_BLE_HEADSET) return true
            if (type == AudioDeviceInfo.TYPE_BLE_SPEAKER) return true
        }
        return false
    }

    private fun AudioDeviceInfo.stableBluetoothKey(): String {
        if (Build.VERSION.SDK_INT >= 28) {
            val address = address
            if (!address.isNullOrBlank() && address != "00:00:00:00:00:00") {
                return address
            }
        }
        val name = productName?.toString()?.takeIf { it.isNotBlank() }
        if (name != null) return "name:${name.hashCode()}"
        return "bt_$id"
    }
}
