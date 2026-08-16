package by.tigre.media.platform.playback.eq

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DesktopAudioRouteMonitor : AudioRouteMonitor {
    override val currentRoute: StateFlow<AudioRouteId> =
        MutableStateFlow(AudioRouteId.DesktopDefault).asStateFlow()
}
