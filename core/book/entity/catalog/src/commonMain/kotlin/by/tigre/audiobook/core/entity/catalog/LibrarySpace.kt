package by.tigre.audiobook.core.entity.catalog

import kotlinx.serialization.Serializable

@Serializable
data class LibrarySpace(
    val id: Id,
    val name: String,
    val icon: String,
    val sortOrder: Int,
    val isDefault: Boolean,
) {
    @JvmInline
    @Serializable
    value class Id(val value: Long)

    companion object {
        const val DEFAULT_NAME = "Основное"
        const val DEFAULT_ICON = "home"
    }
}
