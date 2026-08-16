package by.tigre.media.platform.playback.eq

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Content keys that apps can update after DI is wired
 * (AudioBook → [setBook], Music → [setMusic]).
 */
class MutableEqContentKeyProvider : EqContentKeyProvider {
    private val _contentKey = MutableStateFlow<EqContentKey>(EqContentKey.None)
    private val _folderKey = MutableStateFlow<EqContentKey.Folder?>(null)
    private val _bookId = MutableStateFlow<Long?>(null)
    private val _artistKey = MutableStateFlow<EqContentKey.Artist?>(null)
    private val _albumId = MutableStateFlow<Long?>(null)

    override val contentKey: StateFlow<EqContentKey> = _contentKey.asStateFlow()
    override val folderKey: StateFlow<EqContentKey.Folder?> = _folderKey.asStateFlow()
    override val bookId: StateFlow<Long?> = _bookId.asStateFlow()
    override val artistKey: StateFlow<EqContentKey.Artist?> = _artistKey.asStateFlow()
    override val albumId: StateFlow<Long?> = _albumId.asStateFlow()

    fun clear() {
        _contentKey.value = EqContentKey.None
        _folderKey.value = null
        _bookId.value = null
        _artistKey.value = null
        _albumId.value = null
    }

    fun setBook(bookId: Long, folderUri: String, subPath: String) {
        _bookId.value = bookId
        _folderKey.value = EqContentKey.Folder(folderUri, subPath)
        _artistKey.value = null
        _albumId.value = null
        _contentKey.value = EqContentKey.Book(bookId)
    }

    fun setMusic(artistId: Long, albumId: Long) {
        _artistKey.value = EqContentKey.Artist(artistId)
        _albumId.value = albumId
        _bookId.value = null
        _folderKey.value = null
        _contentKey.value = EqContentKey.Album(albumId)
    }
}
