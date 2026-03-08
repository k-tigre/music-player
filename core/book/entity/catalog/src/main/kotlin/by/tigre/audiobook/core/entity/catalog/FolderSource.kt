package by.tigre.audiobook.core.entity.catalog

data class FolderSource(
    val id: Id,
    val uri: String,
    val name: String,
) {

    @JvmInline
    value class Id(val value: Long)
}
