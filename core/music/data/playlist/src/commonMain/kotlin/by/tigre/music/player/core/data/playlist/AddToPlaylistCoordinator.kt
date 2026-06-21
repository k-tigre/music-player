package by.tigre.music.player.core.data.playlist

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AddToPlaylistCoordinator {
    private val _request = MutableStateFlow<AddToPlaylistRequest?>(null)
    val request: StateFlow<AddToPlaylistRequest?> = _request.asStateFlow()

    fun show(request: AddToPlaylistRequest) {
        _request.value = request
    }

    fun dismiss() {
        _request.value = null
    }
}
