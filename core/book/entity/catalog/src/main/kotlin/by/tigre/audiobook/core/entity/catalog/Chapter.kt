package by.tigre.audiobook.core.entity.catalog

data class Chapter(
    val id: Id,
    val bookId: Book.Id,
    val title: String,
    val fileUri: String,
    val duration: Long,
    val sortOrder: Int,
) {

    @JvmInline
    value class Id(val value: Long)
}
