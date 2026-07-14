package by.tigre.music.player.car

import android.content.Context
import android.net.Uri
import by.tigre.media.platform.background.R
import by.tigre.music.player.core.data.catalog.android.AndroidAlbumArtProvider
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.media.platform.background.car.CarBrowseActions
import by.tigre.media.platform.background.car.CarBrowseItem
import by.tigre.media.platform.background.car.CarMediaIds
import by.tigre.media.platform.background.car.CarMediaLibrary
import kotlinx.coroutines.flow.first

class MusicCarMediaLibrary(
    private val context: Context,
    private val catalog: CatalogSource,
    private val playback: PlaybackController,
) : CarMediaLibrary {

    private val albumArtProvider = AndroidAlbumArtProvider()

    override suspend fun getChildren(parentId: String): List<CarBrowseItem> = when (parentId) {
        CarMediaIds.ROOT -> listOf(
            browsableTab(CarMediaIds.TAB_ARTISTS, context.getString(R.string.car_tab_artists)),
            browsableTab(CarMediaIds.TAB_QUEUE, context.getString(R.string.car_tab_queue)),
        )
        CarMediaIds.TAB_ARTISTS -> catalog.getArtists().map { artistItem(it) }
        else -> when {
            parentId.startsWith("artist/") -> {
                val artistId = CarMediaIds.parseArtistId(parentId)?.let { Artist.Id(it) }
                    ?: return emptyList()
                val actions = artistFolderActions(artistId.value)
                val albums = catalog.getAlbums(artistId).map { album ->
                    albumItem(artistId.value, album)
                }
                actions + albums
            }
            parentId.startsWith("album/") -> {
                val (artistIdRaw, albumIdRaw) = CarMediaIds.parseAlbumIds(parentId)
                    ?: return emptyList()
                val artistId = Artist.Id(artistIdRaw)
                val albumId = Album.Id(albumIdRaw)
                val actions = albumFolderActions(artistIdRaw, albumIdRaw)
                val songs = catalog.getSongsByAlbum(artistId, albumId).map { songItem(it) }
                actions + songs
            }
            parentId == CarMediaIds.TAB_QUEUE -> {
                val queue = playback.currentQueue.first()
                queue.map { item -> songItem(item.song) }
            }
            else -> emptyList()
        }
    }

    override fun playMediaId(mediaId: String) {
        CarMediaIds.parseActionAddArtistId(mediaId)?.let {
            playback.addArtistToPlay(Artist.Id(it))
            return
        }
        CarMediaIds.parseActionAddAlbumIds(mediaId)?.let { (artistId, albumId) ->
            playback.addAlbumToPlay(Album.Id(albumId), Artist.Id(artistId))
            return
        }
        CarMediaIds.parseActionPlayArtistId(mediaId)?.let {
            playback.playArtist(Artist.Id(it))
            return
        }
        CarMediaIds.parseActionPlayAlbumIds(mediaId)?.let { (artistId, albumId) ->
            playback.playAlbum(Album.Id(albumId), Artist.Id(artistId))
            return
        }
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

    override fun addMediaIdToQueue(mediaId: String) {
        CarMediaIds.parseAlbumIds(mediaId)?.let { (artistId, albumId) ->
            playback.addAlbumToPlay(Album.Id(albumId), Artist.Id(artistId))
            return
        }
        CarMediaIds.parseArtistId(mediaId)?.let {
            playback.addArtistToPlay(Artist.Id(it))
            return
        }
        CarMediaIds.parseSongId(mediaId)?.let {
            playback.addSongsToPlay(listOf(Song.Id(it)))
        }
    }

    override suspend fun getBrowseItem(mediaId: String): CarBrowseItem? {
        CarMediaIds.parseSongId(mediaId)?.let { id ->
            val song = catalog.getSongById(Song.Id(id)) ?: return null
            return songItem(song)
        }
        CarMediaIds.parseAlbumIds(mediaId)?.let { (artistId, albumId) ->
            val album = catalog.getAlbumById(Artist.Id(artistId), Album.Id(albumId)) ?: return null
            return albumItem(artistId, album)
        }
        CarMediaIds.parseArtistId(mediaId)?.let { id ->
            val artist = catalog.getArtistById(Artist.Id(id)) ?: return null
            return artistItem(artist)
        }
        return null
    }

    override suspend fun search(query: String): List<CarBrowseItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val result = catalog.search(trimmed)
        val artists = result.artists.map { artistItem(it) }
        val songs = result.songs.map { songItem(it) }
        return artists + songs
    }

    private fun artistItem(artist: Artist): CarBrowseItem = CarBrowseItem(
        id = CarMediaIds.artist(artist.id.value),
        title = artist.name,
        isBrowsable = true,
        isPlayable = true,
        customBrowseActionIds = listOf(CarBrowseActions.ADD_TO_QUEUE),
    )

    private fun albumItem(artistId: Long, album: Album): CarBrowseItem = CarBrowseItem(
        id = CarMediaIds.album(artistId, album.id.value),
        title = album.name,
        subtitle = album.years.ifBlank { null },
        isBrowsable = true,
        isPlayable = true,
        artworkUri = albumArtUri(album.id.value),
        customBrowseActionIds = listOf(CarBrowseActions.ADD_TO_QUEUE),
    )

    private fun artistFolderActions(artistId: Long): List<CarBrowseItem> = listOf(
        CarBrowseItem(
            id = CarMediaIds.actionPlayArtist(artistId),
            title = context.getString(R.string.car_action_play_all),
            isBrowsable = false,
            isPlayable = true,
        ),
        CarBrowseItem(
            id = CarMediaIds.actionAddArtist(artistId),
            title = context.getString(R.string.car_action_add_to_queue),
            isBrowsable = false,
            isPlayable = true,
        ),
    )

    private fun albumFolderActions(artistId: Long, albumId: Long): List<CarBrowseItem> = listOf(
        CarBrowseItem(
            id = CarMediaIds.actionPlayAlbum(artistId, albumId),
            title = context.getString(R.string.car_action_play_all),
            isBrowsable = false,
            isPlayable = true,
        ),
        CarBrowseItem(
            id = CarMediaIds.actionAddAlbum(artistId, albumId),
            title = context.getString(R.string.car_action_add_to_queue),
            isBrowsable = false,
            isPlayable = true,
        ),
    )

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

    private fun albumArtUri(albumId: Long): Uri? =
        albumArtProvider.albumArtUri(Album.Id(albumId)) as? Uri
}
