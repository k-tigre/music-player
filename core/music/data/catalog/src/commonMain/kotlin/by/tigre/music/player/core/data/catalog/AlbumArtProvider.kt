package by.tigre.music.player.core.data.catalog

import by.tigre.music.player.core.entiry.catalog.Album

interface AlbumArtProvider {
    fun albumArtUri(albumId: Album.Id): Any?
}
