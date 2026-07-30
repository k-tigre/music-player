package by.tigre.media.platform.entitlements

enum class Tier {
    Free,
    Plus,
    Pro,
    ;

    fun includes(other: Tier): Boolean = ordinal >= other.ordinal
}
