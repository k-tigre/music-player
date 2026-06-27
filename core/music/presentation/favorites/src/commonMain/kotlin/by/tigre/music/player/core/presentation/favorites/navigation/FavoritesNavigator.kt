package by.tigre.music.player.core.presentation.favorites.navigation

import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist

interface FavoritesNavigator {
    fun openCatalog()
    fun openArtist(id: Artist.Id)
    fun openAlbum(artistId: Artist.Id, albumId: Album.Id)
}
