package by.tigre.media.platform.playback.eq

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fallback when the app does not bind content keys. */
class NullEqContentKeyProvider : EqContentKeyProvider {
    override val contentKey: StateFlow<EqContentKey> = MutableStateFlow(EqContentKey.None).asStateFlow()
    override val folderKey: StateFlow<EqContentKey.Folder?> = MutableStateFlow(null).asStateFlow()
    override val bookId: StateFlow<Long?> = MutableStateFlow(null).asStateFlow()
    override val artistKey: StateFlow<EqContentKey.Artist?> = MutableStateFlow(null).asStateFlow()
    override val albumId: StateFlow<Long?> = MutableStateFlow(null).asStateFlow()
}
