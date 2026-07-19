package by.tigre.music.player.core.data.catalog

interface ArtistArtRemote {
    /**
     * Returns a remote image URL for the normalized artist name, or null if not found.
     * Transport / rate-limit errors should be thrown so the caller can back off and retry.
     */
    suspend fun findImageUrl(normalizedName: String): String?
}
