package by.tigre.music.player.core.data.catalog.hidden

import by.tigre.music.player.core.data.storage.preferences.Preferences
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface HiddenCatalogStorage {
    val revision: Flow<Long>
    fun isSongHidden(id: Song.Id): Boolean
    fun isAlbumHidden(artistId: Artist.Id, albumId: Album.Id): Boolean
    fun hideSong(id: Song.Id)
    fun hideAlbum(artistId: Artist.Id, albumId: Album.Id, songIds: List<Song.Id>)
}

internal class HiddenCatalogStorageImpl(
    private val preferences: Preferences,
) : HiddenCatalogStorage {

    private val _revision = MutableStateFlow(0L)
    override val revision: Flow<Long> = _revision.asStateFlow()

    override fun isSongHidden(id: Song.Id): Boolean = loadSongIds().contains(id.value)

    override fun isAlbumHidden(artistId: Artist.Id, albumId: Album.Id): Boolean =
        loadAlbumKeys().contains(albumKey(artistId, albumId))

    override fun hideSong(id: Song.Id) {
        val ids = loadSongIds()
        if (ids.add(id.value)) {
            saveSongIds(ids)
            bump()
        }
    }

    override fun hideAlbum(artistId: Artist.Id, albumId: Album.Id, songIds: List<Song.Id>) {
        val albumKeys = loadAlbumKeys()
        val songs = loadSongIds()
        var changed = albumKeys.add(albumKey(artistId, albumId))
        songIds.forEach { songId ->
            changed = songs.add(songId.value) || changed
        }
        if (changed) {
            saveAlbumKeys(albumKeys)
            saveSongIds(songs)
            bump()
        }
    }

    private fun bump() {
        _revision.value++
    }

    private fun loadSongIds(): MutableSet<Long> {
        val raw = preferences.loadString(KEY_HIDDEN_SONGS, null) ?: return mutableSetOf()
        if (raw.isEmpty()) return mutableSetOf()
        return raw.split(',').mapNotNull { it.toLongOrNull() }.toMutableSet()
    }

    private fun saveSongIds(ids: Set<Long>) {
        preferences.saveString(KEY_HIDDEN_SONGS, ids.joinToString(","))
    }

    private fun loadAlbumKeys(): MutableSet<String> {
        val raw = preferences.loadString(KEY_HIDDEN_ALBUMS, null) ?: return mutableSetOf()
        if (raw.isEmpty()) return mutableSetOf()
        return raw.split(',').filter { it.isNotEmpty() }.toMutableSet()
    }

    private fun saveAlbumKeys(keys: Set<String>) {
        preferences.saveString(KEY_HIDDEN_ALBUMS, keys.joinToString(","))
    }

    private fun albumKey(artistId: Artist.Id, albumId: Album.Id): String =
        "${artistId.value}_${albumId.value}"

    private companion object {
        const val KEY_HIDDEN_SONGS = "catalog_hidden_song_ids"
        const val KEY_HIDDEN_ALBUMS = "catalog_hidden_album_keys"
    }
}
