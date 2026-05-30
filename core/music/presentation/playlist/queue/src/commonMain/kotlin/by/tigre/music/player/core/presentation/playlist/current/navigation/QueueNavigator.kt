package by.tigre.music.player.core.presentation.playlist.current.navigation

import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist

interface QueueNavigator {
    fun onOpenCatalog()
    fun onOpenArtist(artistId: Artist.Id)
    fun onOpenAlbum(artistId: Artist.Id, albumId: Album.Id)
}
