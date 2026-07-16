package by.tigre.media.platform.playback.impl

import by.tigre.media.platform.playback.AudioSpectrumSource
import by.tigre.media.platform.playback.SpectrumFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class NoOpAudioSpectrumSource : AudioSpectrumSource {
    private val _frames = MutableStateFlow<SpectrumFrame?>(null)
    override val frames: StateFlow<SpectrumFrame?> = _frames.asStateFlow()

    override fun setEnabled(enabled: Boolean) = Unit

    override fun release() = Unit
}
