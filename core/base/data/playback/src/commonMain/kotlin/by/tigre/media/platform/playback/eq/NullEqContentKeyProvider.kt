package by.tigre.media.platform.playback.eq

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Music / Desktop: no content scope. */
class NullEqContentKeyProvider : EqContentKeyProvider {
    override val contentKey: StateFlow<EqContentKey> = MutableStateFlow(EqContentKey.None).asStateFlow()
    override val folderKey: StateFlow<EqContentKey.Folder?> = MutableStateFlow(null).asStateFlow()
    override val bookId: StateFlow<Long?> = MutableStateFlow(null).asStateFlow()
}
