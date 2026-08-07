package by.tigre.media.platform.playback.eq

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Content keys that apps can update after DI is wired
 * (e.g. AudioBook binds [updateFromBook] to currentBook).
 */
class MutableEqContentKeyProvider : EqContentKeyProvider {
    private val _contentKey = MutableStateFlow<EqContentKey>(EqContentKey.None)
    private val _folderKey = MutableStateFlow<EqContentKey.Folder?>(null)
    private val _bookId = MutableStateFlow<Long?>(null)

    override val contentKey: StateFlow<EqContentKey> = _contentKey.asStateFlow()
    override val folderKey: StateFlow<EqContentKey.Folder?> = _folderKey.asStateFlow()
    override val bookId: StateFlow<Long?> = _bookId.asStateFlow()

    fun clear() {
        _contentKey.value = EqContentKey.None
        _folderKey.value = null
        _bookId.value = null
    }

    fun setBook(bookId: Long, folderUri: String, subPath: String) {
        _bookId.value = bookId
        _folderKey.value = EqContentKey.Folder(folderUri, subPath)
        _contentKey.value = EqContentKey.Book(bookId)
    }
}
