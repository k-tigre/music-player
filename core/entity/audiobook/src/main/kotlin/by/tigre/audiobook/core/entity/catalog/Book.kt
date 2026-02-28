package by.tigre.audiobook.core.entity.catalog

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Book(
    val id: Id,
    val title: String,
    val folderUri: String,
    val chapterCount: Int,
    val subPath: String,
) : Parcelable {

    @JvmInline
    @Parcelize
    value class Id(val value: Long) : Parcelable
}
