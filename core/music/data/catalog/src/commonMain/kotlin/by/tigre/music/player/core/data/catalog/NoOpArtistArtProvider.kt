package by.tigre.music.player.core.data.catalog

import by.tigre.music.player.core.entiry.catalog.Artist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

object NoOpArtistArtProvider : ArtistArtProvider {
    override fun observe(artistId: Artist.Id, name: String): Flow<ArtistArt> = flowOf(ArtistArt.Missing)
    override fun request(artistId: Artist.Id, name: String) = Unit
}
