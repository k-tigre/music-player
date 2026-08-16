package by.tigre.media.platform.playback

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
     * Outer index matches [presetNames] indices before the first custom entry.
     */
    val builtInPresetBandGainsDb: StateFlow<List<List<Float>>>

    /**
     * Index of the first custom preset in [presetNames], or -1 if unsupported.
     * Custom presets occupy `[customPresetIndex, customPresetIndex + customPresetCount)`.
     */
    val customPresetIndex: StateFlow<Int>

    /** Number of user custom presets (1..3 when supported). */
    val customPresetCount: StateFlow<Int>

    /** Min/max band gain in dB for UI sliders (from hardware on Android). */
    val bandGainRangeDb: StateFlow<Pair<Float, Float>>

    fun selectPreset(index: Int)

    /** Sets one band in dB; selects/writes the active custom preset when supported. */
    fun setBandGainDb(bandIndex: Int, gainDb: Float)

    /**
     * Adds a custom preset by copying current band gains. Returns false if at max capacity
     * or customs are unsupported.
     */
    fun addCustomPreset(): Boolean

    /** Renames a custom preset by absolute [presetNames] index. */
    fun renameCustomPreset(presetIndex: Int, title: String): Boolean
}
