package by.tigre.audiobook.core.entity.catalog

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: Id,
    val title: String,
    val folderUri: String,
    val chapterCount: Int,
    val subPath: String,
    val totalDurationMs: Long = 0,
    val listenedDurationMs: Long = 0,
    val isCompleted: Boolean = false,
    /** Content [android.net.Uri] string from SAF, if a cover was found when scanning. */
    val coverUri: String? = null,
) {

    val progressFraction: Float by lazy {
        if (totalDurationMs > 0) (listenedDurationMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f
    }

    @JvmInline
    @Serializable
    value class Id(val value: Long)
}
