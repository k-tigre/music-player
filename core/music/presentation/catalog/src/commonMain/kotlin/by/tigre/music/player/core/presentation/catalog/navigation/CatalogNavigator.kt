package by.tigre.music.player.core.presentation.catalog.navigation

import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song

interface CatalogNavigator {
    fun showArtists()
    fun showShowAlbums(artist: Artist)
    fun showSongs(album: Album, artist: Artist)
    fun showPreviousScreen()
    fun showSongsForTrack(song: Song)
}
