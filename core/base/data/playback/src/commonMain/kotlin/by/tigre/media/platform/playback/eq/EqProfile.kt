package by.tigre.media.platform.playback.eq

data class EqProfile(
    val id: Long,
    val route: AudioRouteId,
    val content: EqContentKey,
    val presetIndex: Int?,
    val gainsDb: List<Float>,
    val title: String?,
    val updatedAtMs: Long,
)

enum class EqMatchLevel {
    Book,
    Folder,
    Device,
    None,
}

data class EqResolveResult(
    val profile: EqProfile?,
    val matchLevel: EqMatchLevel,
)
