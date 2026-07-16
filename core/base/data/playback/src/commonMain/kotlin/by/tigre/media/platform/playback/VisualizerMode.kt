package by.tigre.media.platform.playback

/**
 * Slot where a visualizer may render. [Mini] is reserved for a future mini-player effect.
 */
enum class VisualizerSlot {
    Full,
    Mini,
}

enum class VisualizerCoverLayout {
    Large,
    Surrounded,
    Minimal,
}

enum class VisualizerMode {
    Off,
    AuraRingCircle,
    AuraRingCenter,
    RadialBars,
    RadialBarsInward,
    RadialBarsOutward,
    SquircleBurst,
    EdgeBurst,
    EdgeBurstTaper,
    EdgeBurstButt,
    SpectrumRibbon,
    LiquidBlob,
    Particles,
    CoverPulse,
    BeatFlash,
    WaveFloor,
    ;

    companion object {
        fun fromStorage(value: String?): VisualizerMode = when (value) {
            "AuraRing" -> AuraRingCircle // legacy surrounded ring
            else -> entries.firstOrNull { it.name == value } ?: Off
        }
    }
}

/**
 * How FFT levels are mapped to bar heights.
 * [Pretty] — per-frame log + min-max (ComposeCircle-style), looks full and lively.
 * [Realistic] — spectrum shape × track PCM loudness (Tee), follows song dynamics / mute-safe.
 */
enum class VisualizerProcessing {
    Pretty,
    Realistic,
    ;

    companion object {
        fun fromStorage(value: String?): VisualizerProcessing =
            entries.firstOrNull { it.name == value } ?: Pretty
    }
}

data class SpectrumFrame(
    val bands: FloatArray,
    val rms: Float,
    val beatPulse: Float,
    val timestampMs: Long,
) {
    // Identity equality so StateFlow always notifies Compose on each capture tick.
    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}
