package by.tigre.music.player.core.data.catalog

/** Coerce nullable/blank MediaStore text columns into non-null display values. */
internal object MediaStoreStrings {
    const val UNKNOWN_ARTIST = "Unknown Artist"
    const val UNKNOWN_ALBUM = "Unknown Album"
    const val UNKNOWN_TITLE = "Unknown"

    fun orDefault(value: String?, default: String): String =
        value?.takeIf { it.isNotBlank() } ?: default
}
