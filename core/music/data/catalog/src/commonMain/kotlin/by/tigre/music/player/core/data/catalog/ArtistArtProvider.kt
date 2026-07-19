package by.tigre.music.player.core.data.catalog

import by.tigre.music.player.core.entiry.catalog.Artist
import kotlinx.coroutines.flow.Flow

interface ArtistArtProvider {
    fun observe(artistId: Artist.Id, name: String): Flow<ArtistArt>
    fun request(artistId: Artist.Id, name: String)
}
