package by.tigre.music.player.core.data.catalog

import android.net.Uri
import by.tigre.music.player.core.data.catalog.android.DbHelper
import by.tigre.music.player.core.data.storage.preferences.Preferences
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface CatalogSource {

    val rootCatalogFolderSelected: StateFlow<String?>
    fun setRootFolder(path: Uri): Boolean

    suspend fun getArtists(): List<Artist>
    suspend fun getAlbums(artistId: Long): List<Album>
    suspend fun getSongsByArtist(artistId: Long): List<Song>
    suspend fun getSongsByAlbum(albumId: Long): List<Song>

    class Impl(
        private val dbHelper: DbHelper,
        private val preferences: Preferences
    ) : CatalogSource {
        private fun processNewRootFolder(path: String) {
            preferences.saveString(KEY_ROOT_FOLDER, path)
        }

        override val rootCatalogFolderSelected = MutableStateFlow(preferences.loadString(KEY_ROOT_FOLDER, null))

        override fun setRootFolder(uri: Uri): Boolean {
            processNewRootFolder(uri.path ?: "")
            rootCatalogFolderSelected.tryEmit(uri.path)
            return true
        }

        override suspend fun getArtists(): List<Artist> = dbHelper.getArtists()
        override suspend fun getAlbums(artistId: Long): List<Album> = dbHelper.getAlbums(artistId)
        override suspend fun getSongsByArtist(artistId: Long): List<Song> = dbHelper.getSongsByArtist(artistId)
        override suspend fun getSongsByAlbum(albumId: Long): List<Song> = dbHelper.getSongsByAlbum(albumId)

        private companion object {
            const val KEY_ROOT_FOLDER = "root_folder"
        }
    }
}
