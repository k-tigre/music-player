package by.tigre.media.platform.playback.prefs

/**
 * In-memory + prefs helper for 1..[EqualizerPreferences.MAX_CUSTOM_PRESETS] custom EQ slots.
 * Callers rebuild [presetNames] via [titles] after mutations.
 */
internal class CustomEqPresetBank(
    private val prefs: EqualizerPreferences,
    private val bandCount: () -> Int,
    private val defaultTitle: String = EqualizerPreferences.DEFAULT_CUSTOM_TITLE,
) {
    private var slots: MutableList<StoredCustomEqPreset> =
        prefs.loadCustomPresets(defaultTitle).toMutableList()

    val count: Int get() = slots.size

    val titles: List<String> get() = slots.map { it.title }

    fun gains(slot: Int): List<Float> {
        val raw = slots.getOrNull(slot)?.gainsDb
        return alignGainsToBandCount(raw, bandCount())
    }

    fun slotIndexForAbsolute(absoluteIndex: Int, firstCustomIndex: Int): Int =
        absoluteIndex - firstCustomIndex

    fun isCustomAbsolute(absoluteIndex: Int, firstCustomIndex: Int): Boolean {
        if (firstCustomIndex < 0) return false
        val slot = absoluteIndex - firstCustomIndex
        return slot in slots.indices
    }

    fun updateGains(slot: Int, gains: List<Float>) {
        if (slot !in slots.indices) return
        slots[slot] = slots[slot].copy(gainsDb = alignGainsToBandCount(gains, bandCount()))
        persist()
    }

    fun rename(slot: Int, title: String): Boolean {
        if (slot !in slots.indices) return false
        val trimmed = title.trim().ifBlank { EqualizerPreferences.defaultCustomTitle(slot, defaultTitle) }
        slots[slot] = slots[slot].copy(title = trimmed)
        persist()
        return true
    }

    /** Copies [gains] into a new slot. Returns new slot index or -1. */
    fun addCopy(gains: List<Float>): Int {
        if (slots.size >= EqualizerPreferences.MAX_CUSTOM_PRESETS) return -1
        val slot = slots.size
        slots.add(
            StoredCustomEqPreset(
                title = EqualizerPreferences.defaultCustomTitle(slot, defaultTitle),
                gainsDb = alignGainsToBandCount(gains, bandCount()),
            ),
        )
        persist()
        return slot
    }

    private fun persist() {
        prefs.saveCustomPresets(slots.toList())
    }
}
