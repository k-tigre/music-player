package by.tigre.music.player.car

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import by.tigre.media.platform.background.R
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.media.platform.background.car.CarBrowseItem
import by.tigre.media.platform.background.car.CarMediaIds
import by.tigre.media.platform.background.car.CarMediaLibrary
import kotlinx.coroutines.flow.first

class MusicCarMediaLibrary(
    private val context: Context,
    private val catalog: CatalogSource,
    private val playback: PlaybackController,
) : CarMediaLibrary {

    override suspend fun getChildren(parentId: String): List<CarBrowseItem> = when (parentId) {
        CarMediaIds.ROOT -> listOf(
            browsableTab(CarMediaIds.TAB_ARTISTS, context.getString(R.string.car_tab_artists)),
            browsableTab(CarMediaIds.TAB_QUEUE, context.getString(R.string.car_tab_queue)),
        )
        CarMediaIds.TAB_ARTISTS -> catalog.getArtists().map { artist ->
            CarBrowseItem(
                id = CarMediaIds.artist(artist.id.value),
                title = artist.name,
                subtitle = null,
                isBrowsable = true,
                isPlayable = false,
            )
        }
        else -> when {
            parentId.startsWith("artist/") -> {
                val artistId = CarMediaIds.parseArtistId(parentId)?.let { Artist.Id(it) } ?: return emptyList()
                catalog.getAlbums(artistId).map { album ->
                    CarBrowseItem(
                        id = CarMediaIds.album(artistId.value, album.id.value),
                        title = album.name,
                        subtitle = album.years.ifBlank { null },
                        isBrowsable = true,
                        isPlayable = false,
                        artworkUri = albumArtUri(album.id.value),
                    )
                }
            }
            parentId.startsWith("album/") -> {
                val (artistIdRaw, albumIdRaw) = CarMediaIds.parseAlbumIds(parentId) ?: return emptyList()
                val artistId = Artist.Id(artistIdRaw)
                val albumId = Album.Id(albumIdRaw)
                catalog.getSongsByAlbum(artistId, albumId).map { song ->
                    songItem(song)
                }
            }
            parentId == CarMediaIds.TAB_QUEUE -> {
                val queue = playback.currentQueue.first()
                queue.map { item -> songItem(item.song) }
            }
            else -> emptyList()
        }
    }

    override fun playMediaId(mediaId: String) {
        CarMediaIds.parseSongId(mediaId)?.let {
            playback.playSong(Song.Id(it))
            return
        }
        CarMediaIds.parseAlbumIds(mediaId)?.let { (artistId, albumId) ->
            playback.playAlbum(Album.Id(albumId), Artist.Id(artistId))
            return
        }
        CarMediaIds.parseArtistId(mediaId)?.let {
            playback.playArtist(Artist.Id(it))
        }
    }

    override suspend fun getBrowseItem(mediaId: String): CarBrowseItem? {
        CarMediaIds.parseSongId(mediaId)?.let { id ->
            val song = catalog.getSongById(Song.Id(id)) ?: return null
            return songItem(song)
        }
        CarMediaIds.parseAlbumIds(mediaId)?.let { (artistId, albumId) ->
            val album = catalog.getAlbumById(Artist.Id(artistId), Album.Id(albumId)) ?: return null
            return CarBrowseItem(
                id = mediaId,
                title = album.name,
                subtitle = album.years.ifBlank { null },
                isBrowsable = true,
                isPlayable = false,
                artworkUri = albumArtUri(album.id.value),
            )
        }
        CarMediaIds.parseArtistId(mediaId)?.let { id ->
            val artist = catalog.getArtistById(Artist.Id(id)) ?: return null
            return CarBrowseItem(
                id = mediaId,
                title = artist.name,
                isBrowsable = true,
                isPlayable = false,
            )
        }
        return null
    }

    private fun songItem(song: Song): CarBrowseItem = CarBrowseItem(
        id = CarMediaIds.song(song.id.value),
        title = song.name,
        subtitle = "${song.artist} · ${song.album}",
        isBrowsable = false,
        isPlayable = true,
        artworkUri = albumArtUri(song.albumId.value),
    )

    private fun browsableTab(id: String, title: String): CarBrowseItem = CarBrowseItem(
        id = id,
        title = title,
        isBrowsable = true,
        isPlayable = false,
    )

    private fun albumArtUri(albumId: Long) =
        ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
}
