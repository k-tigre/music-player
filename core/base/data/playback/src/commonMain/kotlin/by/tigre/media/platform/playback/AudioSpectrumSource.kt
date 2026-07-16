package by.tigre.media.platform.playback

import kotlinx.coroutines.flow.StateFlow

interface AudioSpectrumSource {
    val frames: StateFlow<SpectrumFrame?>

    /** When false, capture is released / idle. */
    fun setEnabled(enabled: Boolean)

    fun release()
}
