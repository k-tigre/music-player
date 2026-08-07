package by.tigre.media.platform.playback.eq

/**
 * Optional content scope for an EQ profile. Music v1 always uses [None].
 * Book v1 uses [Book] / [Folder] from catalog path grouping (`subPath`).
 */
sealed class EqContentKey {
    abstract val kind: Kind
    /** Opaque storage string; empty for [None]. */
    abstract val storageKey: String

    data object None : EqContentKey() {
        override val kind: Kind = Kind.None
        override val storageKey: String = ""
    }

    data class Book(val bookId: Long) : EqContentKey() {
        override val kind: Kind = Kind.Book
        override val storageKey: String = bookId.toString()
    }

    /**
     * Catalog folder group: library root [folderUri] + relative [subPath]
     * (same grouping as AudioBook library UI).
     */
    data class Folder(
        val folderUri: String,
        val subPath: String,
    ) : EqContentKey() {
        override val kind: Kind = Kind.Folder
        override val storageKey: String = "$folderUri\n$subPath"
    }

    enum class Kind(val storageName: String) {
        None("none"),
        Book("book"),
        Folder("folder"),
        ;

        companion object {
            fun fromStorage(name: String): Kind =
                entries.firstOrNull { it.storageName == name } ?: None
        }
    }

    companion object {
        fun fromStorage(kind: Kind, key: String): EqContentKey = when (kind) {
            Kind.None -> None
            Kind.Book -> {
                val id = key.toLongOrNull()
                if (id == null) None else Book(id)
            }
            Kind.Folder -> {
                val nl = key.indexOf('\n')
                if (nl < 0) {
                    if (key.isBlank()) None else Folder(key, "")
                } else {
                    Folder(key.substring(0, nl), key.substring(nl + 1))
                }
            }
        }
    }
}
