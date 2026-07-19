package by.tigre.music.player.core.data.catalog.art

import by.tigre.music.player.core.data.catalog.ArtistArtProvider

object ArtistArtProviders {
    fun create(cacheDirPath: String): ArtistArtProvider {
        val httpClient = createArtistArtHttpClient()
        return QueuedArtistArtProvider(
            cache = ArtistArtDiskCache(cacheDirPath),
            remote = DeezerArtistArtRemote(httpClient),
            httpClient = httpClient,
        )
    }
}
