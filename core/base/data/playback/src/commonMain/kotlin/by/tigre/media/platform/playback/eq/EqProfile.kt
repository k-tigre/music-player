package by.tigre.media.platform.playback.eq

data class EqProfile(
    val id: Long,
    val route: AudioRouteId,
    val content: EqContentKey,
    val presetIndex: Int?,
    val gainsDb: List<Float>,
    val title: String?,
    val updatedAtMs: Long,
    val source: EqProfileSource = EqProfileSource.Manual,
)

enum class EqProfileSource(val storageName: String) {
    Manual("manual"),
    Auto("auto"),
    ;

    companion object {
        fun fromStorage(name: String): EqProfileSource =
            entries.firstOrNull { it.storageName == name } ?: Manual
    }
}

enum class EqMatchLevel {
    Book,
    Folder,
    Album,
    Artist,
    Device,
    None,
}

data class EqResolveResult(
    val profile: EqProfile?,
    val matchLevel: EqMatchLevel,
)

/** Soft toast when EQ was carried from previous content. */
data object EqCarryForwardNotice

/**
 * Lifecycle signals for analytics (no analytics dependency in playback).
 * [outcome] values: exact | carry | device_seed | none
 */
data class EqProfileLifecycleEvent(
    val outcome: String,
    val matchLevel: EqMatchLevel,
    val source: EqProfileSource?,
    val routeKind: String,
    val contentKind: String,
)
