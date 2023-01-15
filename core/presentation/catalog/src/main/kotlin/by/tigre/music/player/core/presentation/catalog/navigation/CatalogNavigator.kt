package by.tigre.music.player.core.presentation.catalog.navigation

import by.tigre.music.player.core.presentation.catalog.entiry.Album
import by.tigre.music.player.core.presentation.catalog.entiry.Artist

interface CatalogNavigator {
    fun showShowAlbums(artist: Artist)
    fun showSongs(album: Album)
    fun showPreviousScreen()
}
