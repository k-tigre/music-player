package by.tigre.music.player.core.data.playback

import kotlinx.coroutines.flow.StateFlow

interface PlaybackEqualizer {
    val isAvailable: StateFlow<Boolean>
    val presetNames: StateFlow<List<String>>
    val selectedPresetIndex: StateFlow<Int>

    /** Center frequency of each band in Hz (same order as [bandGainDb]). */
    val bandCenterHz: StateFlow<List<Float>>

    /** Current gain per band in dB. */
    val bandGainDb: StateFlow<List<Float>>

    /**
     * dB per band for each built-in (non-custom) preset. Empty on platforms where factory presets are opaque.
     * Outer index matches [presetNames] indices before the custom entry.
     */
    val builtInPresetBandGainsDb: StateFlow<List<List<Float>>>

    /** Index of the custom preset in [presetNames], or -1 if unsupported. */
    val customPresetIndex: StateFlow<Int>

    /** Min/max band gain in dB for UI sliders (from hardware on Android). */
    val bandGainRangeDb: StateFlow<Pair<Float, Float>>

    fun selectPreset(index: Int)

    /** Sets one band in dB; selects custom preset when supported. */
    fun setBandGainDb(bandIndex: Int, gainDb: Float)
}
