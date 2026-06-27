package by.tigre.music.player.core.data.catalog.android

import android.content.ContentUris
import android.provider.MediaStore
import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.entiry.catalog.Album

class AndroidAlbumArtProvider : AlbumArtProvider {
    override fun albumArtUri(albumId: Album.Id): Any? =
        ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId.value)
}
