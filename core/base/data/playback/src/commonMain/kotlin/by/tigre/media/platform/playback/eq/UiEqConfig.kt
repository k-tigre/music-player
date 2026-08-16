package by.tigre.media.platform.playback.eq

/**
 * UI graphic-EQ layout and preset source. Shared [AndroidPlaybackEqualizer] maps these
 * centers onto device hardware bands; Music keeps hardware factory presets, AudioBook
 * uses speech [Defined] presets.
 */
data class UiEqConfig(
    val bandCentersHz: FloatArray,
    val presetSource: EqPresetSource,
) {
    companion object {
        /** Same centers as desktop / legacy Music Android UI (11 bands). */
        val MusicBandCentersHz: FloatArray = floatArrayOf(
            32f, 64f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f, 20000f,
        )

        fun musicDefault(): UiEqConfig = UiEqConfig(
            bandCentersHz = MusicBandCentersHz.copyOf(),
            presetSource = EqPresetSource.HardwareFactory,
        )

        fun audiobookSpeech(): UiEqConfig = UiEqConfig(
            bandCentersHz = AudiobookSpeechEq.bandCentersHz.copyOf(),
            presetSource = EqPresetSource.Defined(AudiobookSpeechEq.presets),
        )
    }
}

sealed class EqPresetSource {
    /** Android [android.media.audiofx.Equalizer] factory presets (+ Custom). */
    data object HardwareFactory : EqPresetSource()

    /** App-defined presets applied as UI-band gains (+ Custom). */
    data class Defined(val presets: List<EqPresetDefinition>) : EqPresetSource()
}

data class EqPresetDefinition(
    /** Stable id for localization (e.g. `flat`, `voice`). */
    val id: String,
    /** English fallback label when UI has no string for [id]. */
    val displayName: String,
    val gainsDb: FloatArray,
)
