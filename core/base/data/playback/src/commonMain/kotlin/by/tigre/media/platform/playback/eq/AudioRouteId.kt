package by.tigre.media.platform.playback.eq

/**
 * Stable id for the current audio output. Used as EQ profile partition key.
 */
data class AudioRouteId(
    val kind: Kind,
    /** Opaque stable key within [kind], e.g. BT address/name hash, or fixed constants. */
    val stableKey: String,
) {
    enum class Kind {
        Bluetooth,
        Wired,
        Speaker,
        Other,
        Desktop,
    }

    /** Serialized form stored in DB `route_key`. */
    fun storageKey(): String = "${kind.name.lowercase()}:$stableKey"

    companion object {
        val BuiltinSpeaker = AudioRouteId(Kind.Speaker, "builtin_speaker")
        val WiredHeadset = AudioRouteId(Kind.Wired, "wired_headset")
        val DesktopDefault = AudioRouteId(Kind.Desktop, "desktop_default")
        val Unknown = AudioRouteId(Kind.Other, "unknown")

        fun parse(storageKey: String): AudioRouteId {
            val sep = storageKey.indexOf(':')
            if (sep <= 0) return Unknown.copy(stableKey = storageKey.ifBlank { "unknown" })
            val kind = Kind.entries.firstOrNull {
                it.name.equals(storageKey.substring(0, sep), ignoreCase = true)
            } ?: Kind.Other
            val key = storageKey.substring(sep + 1).ifBlank { "unknown" }
            return AudioRouteId(kind, key)
        }
    }
}
