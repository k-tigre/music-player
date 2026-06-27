package by.tigre.music.player.core.presentation.playlist.library.navigation

import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.playlist.Playlist

interface PlaylistsNavigator {
    fun openDetail(id: Playlist.Id)
    fun showPreviousScreen()
    fun openCatalog()
    fun openQueue()
    fun openArtist(id: Artist.Id)
    fun openAlbum(artistId: Artist.Id, albumId: Album.Id)
}
