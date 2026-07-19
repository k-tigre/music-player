package by.tigre.music.player.core.data.catalog.art

internal object ArtistNameNormalizer {
    fun normalize(name: String): String =
        name.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()
}
